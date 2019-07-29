package com.bergerkiller.bukkit.mw;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class Position extends Location implements Cloneable {
    private String worldname;

    public Position(Location location) {
        this(location.getWorld(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }
    public Position(World world, double x, double y, double z, float yaw, float pitch) {
        super(null, x, y, z, yaw, pitch);
        if (world != null) {
            this.worldname = world.getName();
        }
    }
    public Position(String worldname, double x, double y, double z) {
        this(worldname, x, y, z, 0, 0);
    }
    public Position(String worldname, double x, double y, double z, float yaw, float pitch) {
        super(null, x, y, z, yaw, pitch);
        this.worldname = worldname;
    }

    @Override
    public World getWorld() {
        return Bukkit.getServer().getWorld(this.worldname);
    }

    public String getWorldName() {
        return this.worldname;
    }

    @Override
    public void setWorld(World world) {
        this.setWorldName(world.getName());
    }

    public void setWorldName(String worldname) {
        this.worldname = worldname;
    }
    
    @Override
    public double distanceSquared(Location location) {
        return new Location(location.getWorld(), this.getX(), this.getY(), this.getZ()).distanceSquared(location);
    }
    
    @Override
    public double distance(Location location) {
        return Math.abs(distanceSquared(location));
    }
    
    public Location toLocation() {
        return new Location(this.getWorld(), this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch());
    }

    public Location toLocation(World world) {
        return new Location(world, this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch());
    }

    @Override
    public Block getBlock() {
        return getBlock(getWorld());
    }

    /**
     * Gets the Block, specifying the World to get the Block in
     * 
     * @param world
     * @return Block
     */
    public Block getBlock(World world) {
        return world == null ? null : world.getBlockAt(this);
    }

    @Override
    public Position clone() {
        return new Position(getWorldName(), getX(), getY(), getZ(), getYaw(), getPitch());
    }
}
