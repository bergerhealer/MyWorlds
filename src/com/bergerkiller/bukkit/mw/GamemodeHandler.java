package com.bergerkiller.bukkit.mw;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

public class GamemodeHandler {
	private static HashMap<String, GameMode> worldModes = new HashMap<String, GameMode>();
	
	public static GameMode getMode(String gamemmodename, GameMode def) {
		for (GameMode mode : GameMode.values()) {
			if (mode.name().equalsIgnoreCase(gamemmodename)) {
				return mode;
			}
		}
		return def;
	}
	
	public static GameMode get(World world, GameMode def) {
		return get(world.getName(), def);
	}
	public static GameMode get(String worldname, GameMode def) {
		GameMode mode = worldModes.get(worldname.toLowerCase());
		if (mode == null) return def;
		return mode;
	}
	public static void set(String worldname, GameMode mode) {
		if (mode == null) {
			worldModes.remove(worldname.toLowerCase());
		} else {
			worldModes.put(worldname.toLowerCase(), mode);
		}

	}
	public static void set(String worldname, String modename) {
		set(worldname, getMode(modename, null));
	}
	
	public static void updatePlayer(Player p, World world) {
		GameMode mode = get(world, null);
		if (mode != null) p.setGameMode(mode);
	}
	public static void updatePlayers(World world) {
		for (Player p : world.getPlayers()) {
			updatePlayer(p, world);
		}
	}
	public static void updateAll() {
		for (World w : Bukkit.getServer().getWorlds()) {
			updatePlayers(w);
		}
	}

	public static void save(Configuration config) {
		for (String world : config.getKeys()) {
			if (get(world, null) == null) {
				config.setProperty(world.toLowerCase() + ".gamemode", "none");
			}
		}
		for (Map.Entry<String, GameMode> mode : worldModes.entrySet()) {
			config.setProperty(mode.getKey() + ".gamemode", mode.getValue().name());
		}
	}
	public static void load(Configuration config, String worldname) {
		set(worldname, config.getString(worldname + ".gamemode", "NONE"));
	}
		
}
