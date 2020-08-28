package com.bergerkiller.bukkit.mw.portal;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

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
    public static Matrix4x4 compute(Block netherPortalBlock) {
        BlockData blockData = WorldUtil.getBlockData(netherPortalBlock);
        if (!MaterialUtil.ISNETHERPORTAL.get(blockData)) {
            // No idea what to do with this. Just pick position above and pray?
            Matrix4x4 fallback = new Matrix4x4();
            fallback.translate(netherPortalBlock.getX() + 0.5,
                               netherPortalBlock.getY() + 1.0,
                               netherPortalBlock.getZ() + 0.5);
            return fallback;
        }

        Queue<Block> remaining = new LinkedList<Block>(Collections.singletonList(netherPortalBlock));
        if (blockData.getState("axis", String.class).equalsIgnoreCase("x")) {
            return findAlongX(new HashSet<Block>(), remaining, blockData);
        } else {
            return findAlongZ(new HashSet<Block>(), remaining, blockData);
        }
    }

    private static Matrix4x4 findAlongX(Set<Block> passed, Queue<Block> remaining, BlockData blockData) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int z = remaining.peek().getZ();
        Block next;
        while ((next = remaining.poll()) != null) {
            if (WorldUtil.getBlockData(next) == blockData && passed.add(next)) {
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
        return result;
    }

    private static Matrix4x4 findAlongZ(Set<Block> passed, Queue<Block> remaining, BlockData blockData) {
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int x = remaining.peek().getX();
        Block next;
        while ((next = remaining.poll()) != null) {
            if (WorldUtil.getBlockData(next) == blockData && passed.add(next)) {
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
        return result;
    }
}
