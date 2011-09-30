package com.bergerkiller.bukkit.mw;

import java.util.HashSet;

import org.bukkit.World;
import org.bukkit.util.config.Configuration;

public class PvPData {
	private static HashSet<String> pvpWorlds = new HashSet<String>();
	public static boolean isPvP(String worldname) {
		return pvpWorlds.contains(worldname.toLowerCase());
	}
	public static void updatePvP(World w) {
	    w.setPVP(isPvP(w.getName()));
	}
	
	public static void load(Configuration config, String world) {
		setPvP(world, config.getBoolean(world + ".pvp", true));
	}
	public static void save(Configuration config) {
		for (String worldname : config.getKeys()) {
			config.setProperty(worldname.toLowerCase() + ".pvp", isPvP(worldname));
		}
		for (String world : pvpWorlds) {
			config.setProperty(world + ".pvp", true);
		}
	}
	
	public static void setPvP(String worldname, boolean PvP) {
		if (PvP) {
			pvpWorlds.add(worldname.toLowerCase());
		} else {
			pvpWorlds.remove(worldname.toLowerCase());
		}
		World w = WorldManager.getWorld(worldname);
		if (w != null) w.setPVP(PvP);
	}

}
