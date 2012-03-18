package com.bergerkiller.bukkit.mw;

import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import com.bergerkiller.bukkit.common.utils.EnumUtil;

public enum WorldMode {
	NORMAL(Environment.NORMAL, WorldType.NORMAL, "normal"),
	NETHER(Environment.NETHER, WorldType.NORMAL, "nether"),
	THE_END(Environment.THE_END, WorldType.NORMAL, "the_end"),
	FLAT(Environment.NORMAL, WorldType.FLAT, "flat");
	
	private WorldMode(Environment env, WorldType wtype, String name) {
		this.env = env;
		this.wtype = wtype;
		this.name = name;
	}
	
	private final Environment env;
	private final WorldType wtype;
	private final String name;
	
	public String toString() {
		return this.name;
	}
	
	public Environment getEnvironment() {
		return this.env;
	}
	
	public WorldType getType() {
		return this.wtype;
	}
	
	public void apply(WorldCreator creator) {
		creator.environment(this.env);
		creator.type(this.wtype);
	}
	
	public static WorldMode get(String worldname) {
		return EnumUtil.parse(worldname, NORMAL);
	}
	
	public static WorldMode get(World world) {
		if (world.getWorldType() == WorldType.FLAT) {
			return FLAT;
		} else if (world.getEnvironment() == Environment.NETHER) {
			return NETHER;
		} else if (world.getEnvironment() == Environment.THE_END) {
			return THE_END;
		}
		return NORMAL;
	}
}
