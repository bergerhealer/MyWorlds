package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.MaterialTypeProperty;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.MaterialUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.common.wrappers.BlockData;
import com.bergerkiller.generated.net.minecraft.world.phys.AxisAlignedBBHandle;

public class Util {
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

    /**
     * Attempts to find the type of Portal that is near a specific Block<br>
     * <br>
     * <b>Deprecated: use {@link PortalType#findPortalType(World, int, int, int)}} instead</b>
     * 
     * @param world to look in
     * @param x - coordinate to look nearby
     * @param y - coordinate to look nearby
     * @param z - coordinate to look nearby
     * @return Portal type, or NULL if no Portal is found
     */
    @Deprecated
    public static PortalType findPortalType(World world, int x, int y, int z) {
        return PortalType.findPortalType(world, x, y, z);
    }

    /**
     * Checks if a given block is part of a valid water portal, plugin settings are applied<br>
     * <br>
     * <b>Deprecated: use {@link PortalType#detect(Block)} instead</b>
     * 
     * @param main portal block
     * @return True if it is a water Portal, False if not
     */
    @Deprecated
    public static boolean isWaterPortal(Block main) {
        return PortalType.WATER.detect(main);
    }

    /**
     * Checks if a given block is part of a valid ender portal, plugin settings are applied<br>
     * <br>
     * <b>Deprecated: use {@link PortalType#detect(Block)} instead</b>
     * 
     * @param main portal block
     * @return True if it is an end Portal, False if not
     */
    @Deprecated
    public static boolean isEndPortal(Block main) {
        return PortalType.END.detect(main);
    }

    /**
     * Checks if a given block is part of a valid nether portal, plugin settings are applied<br>
     * <br>
     * <b>Deprecated: use {@link PortalType#detect(Block)} instead</b>
     * 
     * @param main portal block
     * @return True if it is a nether Portal, False if not
     */
    @Deprecated
    public static boolean isNetherPortal(Block main) {
        return PortalType.NETHER.detect(main);
    }

    /**
     * Adjusts a Location position upwards until it is a safe spot for a Player to
     * be teleported/spawned at.
     * 
     * @param location to add to, can be null
     * @param entity Entity to offset the location for. Has special logic for Minecarts
     *               which are 90 degrees rotated for some reason. Can be null.
     * @return Location with the spawn offset
     */
    public static Location spawnOffset(Location location, Entity entity) {
        if (location == null) {
            return null;
        }

        // Safe copy
        location = location.clone();

        // Minecarts are teleported 90 degrees rotated
        //TODO: Mobs?
        double minOffset = 0.001;
        if (entity instanceof Minecart) {
            minOffset = 0.0625;
        }

        Block block = location.getBlock();
        AxisAlignedBBHandle blockBB = WorldUtil.getBlockData(block).getBoundingBox(block);
        for (int n = 0; n < 20; n++) {
            Block above = block.getRelative(BlockFace.UP);
            AxisAlignedBBHandle aboveBB = WorldUtil.getBlockData(above).getBoundingBox(above);
            double floorY = block.getY();
            double ceilY = floorY + 2.0;

            // Adjust with bounding boxes, if they exist
            if (blockBB != null) {
                floorY += blockBB.getMaxY();
            }
            if (aboveBB != null) {
                ceilY = above.getY() + aboveBB.getMinY();
            }
            if ((ceilY - floorY) >= 1.7) {
                // Add a very small amount of offset so the player stands on top, for sure
                location.setY(floorY + minOffset);
                return location;
            }

            // Shift
            block = above;
            blockBB = aboveBB;
        }

        // Failure. Just teleport to sign blindly, with small offset.
        location.setY(location.getY() + minOffset);
        return location;
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

    /**
     * Parses a player name command argument. If @p is used, resolves to the player closest
     * to the executing sender, or if the sender is a Player, returns the sender.
     *
     * @param sender
     * @param playerName
     * @return Player, or null if not found
     */
    public static Player parsePlayerName(CommandSender sender, String playerName) {
        if (playerName.equals("@p")) {
            if (sender instanceof Player) {
                return (Player) sender;
            }

            Location location;
            if (sender instanceof BlockCommandSender) {
                location = ((BlockCommandSender) sender).getBlock().getLocation();
                location.setX(location.getBlockX() + 0.5);
                location.setY(location.getBlockY() + 0.5);
                location.setZ(location.getBlockZ() + 0.5);
            } else if (sender instanceof Entity) {
                location = ((Entity) sender).getLocation();
            } else {
                sender.sendMessage(ChatColor.RED + "Command with @p must be done as Player or CommandBlock");
                return null;
            }

            Iterator<Player> iter = location.getWorld().getPlayers().iterator();
            if (!iter.hasNext()) {
                sender.sendMessage(ChatColor.RED + "No players on this world");
                return null;
            }

            Player bestPlayer = iter.next();
            double bestDistSq = bestPlayer.getLocation().distanceSquared(location);
            while (iter.hasNext()) {
                Player p = iter.next();
                double distSq = p.getLocation().distanceSquared(location);
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    bestPlayer = p;
                }
            }
            return bestPlayer;
        }

        Player p = Bukkit.getPlayer(playerName);
        if (p == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + playerName + "' is not online!");
        }
        return p;
    }

    // Can be removed once BKCommonLib 1.19-v2 or later is a hard-dependency
    public static ItemStack getEquipment(Player player, EquipmentSlot slot) {
        if (Common.hasCapability("Common:EntityUtil:GetSetEquipmentSlot")) {
            return getEquipmentBKCL(player, slot);
        } else {
            return getEquipmentLegacy(player, slot);
        }
    }

    private static ItemStack getEquipmentLegacy(Player player, EquipmentSlot slot) {
        try {
            return player.getEquipment().getItem(slot);
        } catch (Throwable t) {
            return null; // Ew.
        }
    }

    private static ItemStack getEquipmentBKCL(Player player, EquipmentSlot slot) {
        return EntityUtil.getEquipment(player, slot);
    }
}
