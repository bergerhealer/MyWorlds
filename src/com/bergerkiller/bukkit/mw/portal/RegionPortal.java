package com.bergerkiller.bukkit.mw.portal;

import com.bergerkiller.bukkit.common.BlockLocation;

public class RegionPortal extends Portal {

	public int xMin, yMin, zMin, xMax, yMax, zMax;
	public String worldname;

	@Override
	public boolean isIn(BlockLocation location) {
		if (!location.world.equals(this.worldname)) return false; 
		if (location.x > this.xMax) return false;
		if (location.y > this.yMax) return false;
		if (location.z > this.zMax) return false;
		if (location.x < this.xMin) return false;
		if (location.y < this.yMin) return false;
		if (location.z < this.zMin) return false;
		return true;
	}
	
	public void setRegion(BlockLocation pos1, BlockLocation pos2) {
		this.xMin = Math.min(pos1.x, pos2.x);
		this.yMin = Math.min(pos1.y, pos2.y);
		this.zMin = Math.min(pos1.z, pos2.z);
		this.xMax = Math.max(pos1.x, pos2.x);
		this.yMax = Math.max(pos1.y, pos2.y);
		this.zMax = Math.max(pos1.z, pos2.z);
		this.worldname = pos1.world;
	}
	
}
