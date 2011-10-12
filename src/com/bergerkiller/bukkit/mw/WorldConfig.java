package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.util.config.Configuration;

public class WorldConfig {	
	public static void load(String filename) {
		Configuration config = new Configuration(new File(filename));
		config.load();
		for (String worldname : config.getKeys()) {
			worldname = worldname.toLowerCase();
			WorldManager.load(config, worldname);
			
			load(config, worldname);
			
			PvPData.load(config, worldname);
			GamemodeHandler.load(config, worldname);
			TimeControl.load(config, worldname);
			SpawnControl.load(config, worldname);
			Portal.loadDefaultPortals(config, worldname);
		}
	}
	
	private static void load(Configuration config, String worldname) {

		if (config.getBoolean(worldname + ".loaded", false)) {
			if (WorldManager.worldExists(worldname)) {
				if (WorldManager.getOrCreateWorld(worldname) == null) {
					MyWorlds.log(Level.SEVERE, "Failed to (pre)load world: " + worldname);
				}
			} else {
				MyWorlds.log(Level.WARNING, "World: " + worldname + " no longer exists and has not been loaded!");
			}
		}
		if (config.getBoolean(worldname + ".holdWeather", false)) {
			MWWeatherListener.holdWorlds.add(worldname.toLowerCase());
		}
	}
	
	public static void save(String filename) {
		Configuration config = new Configuration(new File(filename));
		config.load();
		WorldManager.save(config);
		PvPData.save(config);
		GamemodeHandler.save(config);
		TimeControl.save(config);
		SpawnControl.save(config);
		Portal.saveDefaultPortals(config);
		
		save(config);
		config.save();
	}
	
	private static void save(Configuration config) {
		for (String worldname : config.getKeys()) {
			config.setProperty(worldname.toLowerCase() + ".loaded", false);
			config.setProperty(worldname.toLowerCase() + ".holdWeather", false);
		}
		for (World w : Bukkit.getServer().getWorlds()) {
			config.setProperty(w.getName().toLowerCase() + ".loaded", true);
		}
		for (String worldname : MWWeatherListener.holdWorlds) {
			config.setProperty(worldname + ".holdWeather", true);
		}
	}

}
