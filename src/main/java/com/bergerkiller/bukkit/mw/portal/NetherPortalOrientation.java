package com.bergerkiller.bukkit.mw.portal;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.bergerkiller.bukkit.common.bases.IntVector3;
import com.bergerkiller.bukkit.common.math.Matrix4x4;
import com.bergerkiller.bukkit.common.utils.MaterialUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.common.wrappers.BlockData;

/**
 * Computes the orientation transformation of a nether portal.
 * Uses the passed ender portal block to compute the overall shape of the portal,
 * then creates a transformation matrix at the bottom-middle facing north.
 */
public class NetherPortalOrientation {
    private final World world;
    private final Set<IntVector3> portalBlocks;
    private final Matrix4x4 transform;

    private NetherPortalOrientation(World world, Set<IntVector3> portalBlocks, Matrix4x4 transform) {
        this.world = world;
        this.portalBlocks = portalBlocks;
        this.transform = transform;
    }

    /**
     * Gets the world this nether portal is on
     * 
     * @return world
     */
    public World getWorld() {
        return this.world;
    }

    /**
     * Gets the portal blocks making up the portal
     * 
     * @return portal blocks
     */
    public Set<IntVector3> getPortalBlocks() {
        return this.portalBlocks;
    }

    /**
     * Gets the world transformation matrix for the base
     * position of the portal.
     * 
     * @return transform
     */
    public Matrix4x4 getTransform() {
        return this.transform;
    }

    /**
     * Adjusts a position to be within a portal block making up this nether portal frame.
     * The input location object is modified.
     * 
     * @param location
     */
    public void adjustPosition(Location location) {
        if (this.portalBlocks.isEmpty() || this.portalBlocks.contains(new IntVector3(location))) {
            return; // fallback or contained inside, do nothing
        }

        // Find a portal block that is closest to this location
        // Prefer one that has a portal block above as well (safe)
        boolean found_safe = false;
        double best_dest = Double.MAX_VALUE;
        IntVector3 best = null;
        for (IntVector3 pos : this.portalBlocks) {
            double dx = pos.midX()-location.getX();
            double dy = pos.y-location.getY();
            double dz = pos.midZ()-location.getZ();
            double dsq = (dx*dx) + (dy*dy) + (dz*dz);

            if (this.portalBlocks.contains(pos.add(BlockFace.UP))) {
                if (!found_safe) {
                    found_safe = true;
                } else if (dsq >= best_dest) {
                    continue;
                }

                best = pos;
                best_dest = dsq;
                continue;
            }

            if (!found_safe && dsq < best_dest) {
                best = pos;
                best_dest = dsq;
            }
        }

        location.setX(best.midX());
        location.setY(best.y);
        location.setZ(best.midZ());
    }

    /**
     * Computes the orientation of a nether portal
     * 
     * @param netherPortalBlock
     * @return orientation
     */
    public static BlockFace computeOrientation(Block netherPortalBlock) {
        BlockData blockData = WorldUtil.getBlockData(netherPortalBlock);
        if (!MaterialUtil.ISNETHERPORTAL.get(blockData)) {
            return BlockFace.SELF;
        } else if (blockData.getState("axis", String.class).equalsIgnoreCase("x")) {
            return BlockFace.NORTH;
        } else {
            return BlockFace.EAST;
        }
    }

    /**
     * Computes the transformation matrix from the original of the world, to the
     * middle-bottom position of the portal.
     * 
     * @param netherPortalBlock
     * @return transformation matrix for the middle-bottom position
     */
    public static NetherPortalOrientation compute(Block netherPortalBlock) {
        BlockData blockData = WorldUtil.getBlockData(netherPortalBlock);
        if (!MaterialUtil.ISNETHERPORTAL.get(blockData)) {
            // No idea what to do with this. Just pick position above and pray?
            Matrix4x4 fallback = new Matrix4x4();
            fallback.translate(netherPortalBlock.getX() + 0.5,
                               netherPortalBlock.getY() + 1.0,
                               netherPortalBlock.getZ() + 0.5);
            return new NetherPortalOrientation(netherPortalBlock.getWorld(), Collections.emptySet(), fallback);
        }

        Queue<Block> remaining = new LinkedList<Block>(Collections.singletonList(netherPortalBlock));
        if (blockData.getState("axis", String.class).equalsIgnoreCase("x")) {
            return findAlongX(remaining, blockData);
        } else {
            return findAlongZ(remaining, blockData);
        }
    }

    private static NetherPortalOrientation findAlongX(Queue<Block> remaining, BlockData blockData) {
        Set<IntVector3> portalBlocks = new HashSet<IntVector3>();
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int z = remaining.peek().getZ();
        World world = remaining.peek().getWorld();
        Block next;
        while ((next = remaining.poll()) != null) {
            if (WorldUtil.getBlockData(next) == blockData && portalBlocks.add(new IntVector3(next))) {
                minX = Math.min(minX, next.getX());
                maxX = Math.max(maxX, next.getX());
                minY = Math.min(minY, next.getY());
                remaining.add(next.getRelative(BlockFace.EAST));
                remaining.add(next.getRelative(BlockFace.WEST));
                remaining.add(next.getRelative(BlockFace.UP));
                remaining.add(next.getRelative(BlockFace.DOWN));
            }
        }

        Matrix4x4 result = new Matrix4x4();
        result.translate(0.5 * ((double) minX + (double) maxX + 1.0),
                         (double) minY,
                         (double) z + 0.5);
        return new NetherPortalOrientation(world, portalBlocks, result);
    }

    private static NetherPortalOrientation findAlongZ(Queue<Block> remaining, BlockData blockData) {
        Set<IntVector3> portalBlocks = new HashSet<IntVector3>();
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int x = remaining.peek().getX();
        World world = remaining.peek().getWorld();
        Block next;
        while ((next = remaining.poll()) != null) {
            if (WorldUtil.getBlockData(next) == blockData && portalBlocks.add(new IntVector3(next))) {
                minZ = Math.min(minZ, next.getZ());
                maxZ = Math.max(maxZ, next.getZ());
                minY = Math.min(minY, next.getY());
                remaining.add(next.getRelative(BlockFace.NORTH));
                remaining.add(next.getRelative(BlockFace.SOUTH));
                remaining.add(next.getRelative(BlockFace.UP));
                remaining.add(next.getRelative(BlockFace.DOWN));
            }
        }

        Matrix4x4 result = new Matrix4x4();
        result.translate((double) x + 0.5,
                         (double) minY,
                         0.5 * ((double) minZ + (double) maxZ + 1.0));
        result.rotateY(0.0, 1.0);
        return new NetherPortalOrientation(world, portalBlocks, result);
    }
}
