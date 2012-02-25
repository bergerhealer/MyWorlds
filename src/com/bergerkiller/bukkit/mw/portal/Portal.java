package com.bergerkiller.bukkit.mw.portal;

import com.bergerkiller.bukkit.common.BlockLocation;
import com.bergerkiller.bukkit.mw.Position;

public abstract class Portal {
	
	public String name;
	public Position position;
	public abstract boolean isIn(BlockLocation location);

}
