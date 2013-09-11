package com.bergerkiller.bukkit.mw;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.config.FileConfiguration;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

public class WorldInventory {
	private static final Set<WorldInventory> inventories = new HashSet<WorldInventory>();
	private static int counter = 0;
	private final Set<String> worlds = new HashSet<String>();
	private String worldname;
	private String name;
	
	public static Collection<WorldInventory> getAll() {
		return inventories;
	}

	public static void load() {
		inventories.clear();
		FileConfiguration config = new FileConfiguration(MyWorlds.plugin, "inventories.yml");
		config.load();
		for (ConfigurationNode node : config.getNodes()) {
			String sharedWorld = node.get("folder", String.class, null);
			if (sharedWorld == null) {
				continue;
			}
			List<String> worlds = node.getList("worlds", String.class);
			// Verify these worlds
			if (worlds.isEmpty()) {
				continue;
			}
			// Verify shared world container
			if (!WorldUtil.getWorldFolder(sharedWorld).exists()) {
				sharedWorld = getSharedWorldName(worlds);
				if (sharedWorld == null) {
					continue;
				}
			}
			WorldInventory inv = new WorldInventory(sharedWorld);
			inv.name = node.getName();
			for (String world : worlds) {
				inv.add(world);
			}
		}
	}

	public static void save() {
		FileConfiguration config = new FileConfiguration(MyWorlds.plugin, "inventories.yml");
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
		this.worldname = WorldManager.matchWorld(worldFolder);
	}

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

	private static String getSharedWorldName(Collection<String> worlds) {
		for (String world : worlds) {
			if (WorldUtil.getWorldFolder(world).exists()) {
				return world;
			}
		}
		return null;
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
			this.worldname = getSharedWorldName(this.worlds);
			if (this.worldname == null) {
				inventories.remove(this);
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
