package com.bergerkiller.bukkit.mw;

import java.io.File;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.bergerkiller.bukkit.common.MaterialTypeProperty;
import com.bergerkiller.bukkit.common.utils.MaterialUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.common.wrappers.BlockData;

public class Util {
    public static final MaterialTypeProperty IS_NETHER_PORTAL = new MaterialTypeProperty("NETHER_PORTAL", "LEGACY_PORTAL");
    public static final MaterialTypeProperty IS_END_PORTAL = new MaterialTypeProperty("END_PORTAL", "LEGACY_ENDER_PORTAL");
    public static final MaterialTypeProperty IS_OBSIDIAN = new MaterialTypeProperty("OBSIDIAN", "LEGACY_OBSIDIAN");
    public static final MaterialTypeProperty IS_AIR = new MaterialTypeProperty("AIR", "LEGACY_AIR");
    public static final MaterialTypeProperty IS_ICE = new MaterialTypeProperty("ICE", "LEGACY_ICE");
    public static final MaterialTypeProperty IS_SNOW = new MaterialTypeProperty("SNOW", "LEGACY_SNOW");

    public static boolean isSolid(Block b, BlockFace direction) {
        int maxwidth = 10;
        while (maxwidth-- >= 0) {
            BlockData data = WorldUtil.getBlockData(b);
            if (MaterialUtil.ISWATER.get(data)) {
                b = b.getRelative(direction);
            } else {
                return !IS_AIR.get(data);
            }
        }
        return false;
    }

    private static boolean isObsidianPortal(Block main, BlockFace direction) {
        for (int counter = 0; counter < 20; counter++) {
            BlockData data = WorldUtil.getBlockData(main);
            if (IS_NETHER_PORTAL.get(data)) {
                main = main.getRelative(direction);
            } else if (IS_OBSIDIAN.get(data)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * Attempts to find the type of Portal that is near a specific Block
     * 
     * @param world to look in
     * @param x - coordinate to look nearby
     * @param y - coordinate to look nearby
     * @param z - coordinate to look nearby
     * @return Portal type, or NULL if no Portal is found
     */
    public static PortalType findPortalType(World world, int x, int y, int z) {
        // Check self
        PortalType type = findPortalTypeSingle(world, x, y, z);
        if (type == null) {
            // Check in a 3x3x3 cube area
            int dx, dy, dz;
            for (dx = -1; dx <= 1; dx++) {
                for (dy = -1; dy <= 1; dy++) {
                    for (dz = -1; dz <= 1; dz++) {
                        type = findPortalTypeSingle(world, x + dx, y + dy, z + dz);
                        if (type != null) {
                            return type;
                        }
                    }
                }
            }
        }
        return type;
    }

    private static PortalType findPortalTypeSingle(World world, int x, int y, int z) {
        BlockData data = WorldUtil.getBlockData(world, x, y, z);
        if (MaterialUtil.ISWATER.get(data)) {
            if (isWaterPortal(world.getBlockAt(x, y, z))) {
                return PortalType.WATER;
            }
        } else if (IS_NETHER_PORTAL.get(data)) {
            if (isNetherPortal(world.getBlockAt(x, y, z))) {
                return PortalType.NETHER;
            }
        } else if (IS_END_PORTAL.get(data)) {
            if (isEndPortal(world.getBlockAt(x, y, z))) {
                return PortalType.END;
            }
        }
        return null;
    }

    /**
     * Checks if a given block is part of a valid water portal, plugin settings are applied
     * 
     * @param main portal block
     * @return True if it is a water Portal, False if not
     */
    public static boolean isWaterPortal(Block main) {
        if (!MyWorlds.useWaterTeleport || !MaterialUtil.ISWATER.get(main)) {
            return false;
        }
        if (MaterialUtil.ISWATER.get(main.getRelative(BlockFace.UP)) || MaterialUtil.ISWATER.get(main.getRelative(BlockFace.DOWN))) {
            if (IS_AIR.get(main.getRelative(BlockFace.NORTH)) || IS_AIR.get(main.getRelative(BlockFace.SOUTH))) {
                if (Util.isSolid(main, BlockFace.WEST) && Util.isSolid(main, BlockFace.EAST)) {
                    return true;
                }
            } else if (IS_AIR.get(main.getRelative(BlockFace.EAST)) || IS_AIR.get(main.getRelative(BlockFace.WEST))) {
                if (Util.isSolid(main, BlockFace.NORTH) && Util.isSolid(main, BlockFace.SOUTH)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if a given block is part of a valid ender portal, plugin settings are applied
     * 
     * @param main portal block
     * @return True if it is an end Portal, False if not
     */
    public static boolean isEndPortal(Block main) {
        return IS_END_PORTAL.get(main);
    }

    /**
     * Checks if a given block is part of a valid nether portal, plugin settings are applied
     * 
     * @param main portal block
     * @param overrideMainType - True to override the main block type checking
     * @return True if it is a nether Portal, False if not
     */
    public static boolean isNetherPortal(Block main) {
        if (!IS_NETHER_PORTAL.get(main)) {
            return false;
        }

        if (!MyWorlds.onlyObsidianPortals) {
            // Simple check
            return true;
        }

        if (isObsidianPortal(main, BlockFace.UP) && isObsidianPortal(main, BlockFace.DOWN)) {
            if (isObsidianPortal(main, BlockFace.NORTH) && isObsidianPortal(main, BlockFace.SOUTH)) {
                return true;
            }
            if (isObsidianPortal(main, BlockFace.EAST) && isObsidianPortal(main, BlockFace.WEST)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the spawn offset to a given Location
     * 
     * @param location to add to, can be null
     * @return Location with the spawn offset
     */
    public static Location spawnOffset(Location location) {
        if (location == null) {
            return null;
        }
        return location.clone().add(0.5, 2, 0.5);
    }

    /**
     * Gets the Location from a Position
     * 
     * @param position to convert
     * @return the Location, or null on failure
     */
    public static Location getLocation(Position position) {
        if (position != null) {
            Location loc = position.toLocation();
            if (loc.getWorld() != null) {
                return loc;
            }
        }
        return null;
    }

    /**
     * Removes all unallowed characters from a portal name.
     * These are characters that would cause internal loading/saving issues otherwise.
     * 
     * @param name to filter
     * @return filtered name
     */
    public static String filterPortalName(String name) {
        if (name == null) {
            return null;
        } else {
            return name.replace("\"", "").replace("'", "");
        }
    }

    /**
     * Gets the amount of bytes of data stored on disk by a specific file or folder
     * 
     * @param file to get the size of
     * @return File/folder size in bytes
     */
    public static long getFileSize(File file) {
        if (!file.exists()) {
            return 0L;
        } else if (file.isDirectory()) {
            long size = 0;
            for (File subfile : file.listFiles()) {
                size += getFileSize(subfile);
            }
            return size;
        } else {
            return file.length();
        }
    }
}
