package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.config.FileConfiguration;

public class WorldInventory {
	private static final Set<WorldInventory> inventories = new HashSet<WorldInventory>();
	private static int counter = 0;

	public static Collection<WorldInventory> getAll() {
		return inventories;
	}

	public static void load(String filename) {
		FileConfiguration config = new FileConfiguration(filename);
		config.load();
		for (ConfigurationNode node : config.getNodes()) {
			String worldFolder = node.get("folder", String.class, null);
			if (worldFolder == null || !WorldManager.worldExists(worldFolder)) {
				continue;
			}
			List<String> worlds = node.getList("worlds", String.class);
			if (worlds.isEmpty()) {
				continue;
			}
			WorldInventory inv = new WorldInventory(worldFolder);
			inv.name = node.getName();
			for (String world : worlds) {
				if (world == null || !WorldManager.worldExists(world)) {
					continue;
				}
				inv.add(world);
			}
		}
	}

	public static void save(String filename) {
		FileConfiguration config = new FileConfiguration(filename);
		Set<String> savedNames = new HashSet<String>();
		for (WorldInventory inventory : inventories) {
			if (inventory.worlds.size() > 1) {
				String name = inventory.name;
				for (int i = 0; i < Integer.MAX_VALUE && !savedNames.add(name.toLowerCase()); i++) {
					name = inventory.name + i;
				}
				ConfigurationNode node = config.getNode(name);
				node.set("folder", inventory.worldname);
				node.set("worlds", new ArrayList<String>(inventory.worlds));
			}
		}
		config.save();
	}

	public static void detach(Collection<String> worldnames) {
		for (String world : worldnames) {
			WorldConfig.get(world).inventory.remove(world, true);
		}
	}

	public static void merge(Collection<String> worldnames) {
		WorldInventory inv = new WorldInventory();
		for (String world : worldnames) {
			inv.add(world);
		}
	}

	private WorldInventory() {
		inventories.add(this);
		this.name = "inv" + counter++;
	}

	public WorldInventory(String worldFolder) {
		this();
		this.worldname = worldFolder;
		this.folder = null;
		worldFolder = WorldManager.matchWorld(worldFolder);
		if (worldFolder != null) {
			this.worldname = worldFolder;
			this.folder = new File(Bukkit.getWorldContainer(), worldFolder);
			this.folder = new File(this.folder, "players");
		}
	}

	private final Set<String> worlds = new HashSet<String>();
	private File folder;
	private String worldname;
	private String name;

	public Collection<String> getWorlds() {
		return this.worlds;
	}

	/**
	 * Gets the World name in which all the inventories of this bundle are saved
	 * 
	 * @return shared world name
	 */
	public String getSharedWorldName() {
		return this.worldname;
	}

	public WorldInventory remove(String worldname, boolean createNew) {
		if (this.worlds.remove(worldname.toLowerCase())) {
			//constructor handles world config update
			if (createNew) {
				new WorldInventory(worldname).add(worldname);
			}
		}
		if (this.worlds.isEmpty()) {
			inventories.remove(this);
		} else if (this.worldname.equalsIgnoreCase(worldname)) {
			for (String world : this.worlds) {
				this.worldname = world;
				break;
			}
		}
		return this;
	}

	public WorldInventory add(String worldname) {
		WorldConfig config = WorldConfig.get(worldname);
		if (config.inventory != null) {
			config.inventory.remove(config.worldname, false);
		}
		config.inventory = this;
		this.worlds.add(worldname.toLowerCase());
		if (this.worldname == null) {
			this.worldname = worldname;
		}
		return this;
	}
}
