package com.bergerkiller.bukkit.mw;

import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.config.FileConfiguration;

public class WorldConfigStore {
	protected static HashMap<String, WorldConfig> worldConfigs = new HashMap<String, WorldConfig>();
	public static WorldConfig get(String worldname) {
		WorldConfig c = worldConfigs.get(worldname.toLowerCase());
		if (c == null) {
			c = new WorldConfig(worldname);
		}
		return c;
	}
	public static WorldConfig get(World world) {
		return get(world.getName());
	}
	public static WorldConfig get(Entity entity) {
		return get(entity.getWorld());
	}
	public static WorldConfig get(Location location) {
		return get(location.getWorld());
	}
	public static WorldConfig get(Block block) {
		return get(block.getWorld());
	}
	public static Collection<WorldConfig> all() {
		return worldConfigs.values();
	}
	public static boolean exists(String worldname) {
		return worldConfigs.containsKey(worldname.toLowerCase());
	}
	public static void init(String filename) {
		FileConfiguration config = new FileConfiguration(filename);
		config.load();
		for (ConfigurationNode node : config.getNodes()) {
			if (WorldManager.worldExists(node.getName())) {
				WorldConfig wc = new WorldConfig(node);
				if (node.get("loaded", false)) {
					wc.loadWorld();
				}
			} else {
				MyWorlds.plugin.log(Level.WARNING, "World: " + node.getName() + " no longer exists, data will be wiped when disabling!");
			}
		}
		for (World world : Bukkit.getServer().getWorlds()) {
			get(world).update(world);
		}
	}
	public static void saveAll(String filename) {
		FileConfiguration cfg = new FileConfiguration(filename);
		for (WorldConfig wc : all()) {
			wc.save(cfg.getNode(wc.worldname));
		}
		cfg.save();
	}
	public static void deinit(String filename) {
		saveAll(filename);
		for (WorldConfig world : all()) {
			world.timeControl.setLocking(false);
		}
	}

	public static void remove(String worldname) {
		worldConfigs.remove(worldname.toLowerCase());
	}
}
