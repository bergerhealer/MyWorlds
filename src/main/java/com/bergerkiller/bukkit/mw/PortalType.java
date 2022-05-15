package com.bergerkiller.bukkit.mw;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.bergerkiller.bukkit.common.math.Matrix4x4;
import com.bergerkiller.bukkit.common.utils.FaceUtil;
import com.bergerkiller.bukkit.common.utils.MaterialUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.common.wrappers.BlockData;
import com.bergerkiller.bukkit.mw.portal.NetherPortalOrientation;

/**
 * Type of Portal material
 */
public enum PortalType {
    NETHER("world.setnetherportal", TeleportCause.NETHER_PORTAL) {
        @Override
        public boolean isEnabled() {
            return MyWorlds.netherPortalEnabled;
        }

        @Override
        public boolean detect(Block main) {
            if (!MyWorlds.netherPortalEnabled || !MaterialUtil.ISNETHERPORTAL.get(main)) {
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
    },
    END("world.setendportal", TeleportCause.END_PORTAL) {
        @Override
        public boolean isEnabled() {
            return MyWorlds.endPortalEnabled;
        }

        @Override
        public boolean detect(Block block) {
            return MyWorlds.endPortalEnabled && MaterialUtil.ISENDPORTAL.get(block);
        }
    },
    WATER("world.setwaterportal", TeleportCause.PLUGIN) {
        @Override
        public boolean isEnabled() {
            return MyWorlds.waterPortalEnabled;
        }

        @Override
        public boolean detect(Block main) {
            if (!MyWorlds.waterPortalEnabled || !MaterialUtil.ISWATER.get(main)) {
                return false;
            }
            if (MaterialUtil.ISWATER.get(main.getRelative(BlockFace.UP)) || MaterialUtil.ISWATER.get(main.getRelative(BlockFace.DOWN))) {
                if (MaterialUtil.ISAIR.get(main.getRelative(BlockFace.NORTH)) || MaterialUtil.ISAIR.get(main.getRelative(BlockFace.SOUTH))) {
                    if (Util.isSolid(main, BlockFace.WEST) && Util.isSolid(main, BlockFace.EAST)) {
                        return true;
                    }
                } else if (MaterialUtil.ISAIR.get(main.getRelative(BlockFace.EAST)) || MaterialUtil.ISAIR.get(main.getRelative(BlockFace.WEST))) {
                    if (Util.isSolid(main, BlockFace.NORTH) && Util.isSolid(main, BlockFace.SOUTH)) {
                        return true;
                    }
                }
            }
            return false;
        }
    };

    private final String _commandNode;
    private final TeleportCause _teleportCause;

    private PortalType(String commandNode, TeleportCause teleportCause) {
        this._commandNode = commandNode;
        this._teleportCause = teleportCause;
    }

    /**
     * Tries to detect this portal type at a Block
     * 
     * @param block
     * @return True if the portal type is detected
     */
    public abstract boolean detect(Block block);

    /**
     * Gets whether this type of portal is enabled in MyWorlds configuration
     *
     * @return True if enabled
     */
    public abstract boolean isEnabled();

    /**
     * Gets the teleport cause when this portal type is used to teleport
     * 
     * @return teleport cause
     */
    public TeleportCause getTeleportCause() {
        return this._teleportCause;
    }

    /**
     * Gets the command node path for the command used to
     * modify this portal type of a world.
     * 
     * @return command node
     */
    public String getCommandNode() {
        return this._commandNode;
    }

    /**
     * Computes the orientation of a portal of this portal type
     * 
     * @param portalBlock
     * @param entity
     * @return orientation
     */
    public BlockFace getOrientation(Block portalBlock, Entity entity) {
        switch (this) {
        case NETHER:
            return NetherPortalOrientation.computeOrientation(portalBlock);
        default:
            if (entity instanceof LivingEntity) {
                return FaceUtil.yawToFace(((LivingEntity) entity).getEyeLocation().getYaw());
            } else {
                return FaceUtil.yawToFace(entity.getLocation().getYaw());
            }
        }
    }

    /**
     * Computes the transformation matrix for the portal position
     * and rotation information in the world
     * 
     * @param portalBlock Block of the portal
     * @param entity Entity that wants to use the portal
     * @return transformation matrix
     */
    public Matrix4x4 getTransform(Block portalBlock, Entity entity) {
        switch (this) {
        case NETHER:
            return NetherPortalOrientation.compute(portalBlock).getTransform();
        default:
            return Matrix4x4.fromLocation(entity.getLocation());
        }
    }

    /**
     * Attempts to find the type of Portal that is near a specific Block
     * 
     * @param block Block to look nearby of
     * @return Portal type, or NULL if no Portal is found
     */
    public static PortalType findPortalType(Block block) {
        return findPortalType(block.getWorld(), block.getX(), block.getY(), block.getZ());
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
            if (WATER.detect(world.getBlockAt(x, y, z))) {
                return WATER;
            }
        } else if (MaterialUtil.ISNETHERPORTAL.get(data)) {
            if (NETHER.detect(world.getBlockAt(x, y, z))) {
                return NETHER;
            }
        } else if (MaterialUtil.ISENDPORTAL.get(data)) {
            if (END.detect(world.getBlockAt(x, y, z))) {
                return END;
            }
        }
        return null;
    }

    private static boolean isObsidianPortal(Block main, BlockFace direction) {
        for (int counter = 0; counter < 20; counter++) {
            BlockData data = WorldUtil.getBlockData(main);
            if (MaterialUtil.ISNETHERPORTAL.get(data)) {
                main = main.getRelative(direction);
            } else if (Util.IS_OBSIDIAN.get(data)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}
