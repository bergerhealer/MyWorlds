package com.bergerkiller.bukkit.mw;

import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.World;

public class PvPData {
	private static HashSet<String> pvpWorlds = new HashSet<String>();
	public static boolean isPvP(String worldname) {
		return pvpWorlds.contains(worldname.toLowerCase());
	}
	public static void updatePvP(World w) {
	    w.setPVP(isPvP(w.getName()));
	}
	
	public static void load(String filename) {
		SafeReader r = new SafeReader(filename);
		String textline = null;
		while ((textline = r.readNonEmptyLine()) != null) {
			pvpWorlds.add(textline);
		}
		r.close();
		if (r.exists()) {
			for (World w : Bukkit.getServer().getWorlds()) {
				updatePvP(w);
			}
		} else {
			for (World w : Bukkit.getServer().getWorlds()) {
				if (w.getPVP()) pvpWorlds.add(w.getName().toLowerCase());
			}
		}
	}
	public static void save(String filename) {
		SafeWriter w = new SafeWriter(filename);
		for (String world : pvpWorlds) {
			w.writeLine(world);
		}
		w.close();
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
