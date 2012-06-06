package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.server.EntityPlayer;
import net.minecraft.server.IDataManager;
import net.minecraft.server.WorldNBTStorage;
import net.minecraft.server.WorldServer;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.SafeField;
import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.config.FileConfiguration;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

public class WorldInventory {
	private static final SafeField<File> playerField = new SafeField<File>(WorldNBTStorage.class, "playerDir");
	private static final Set<WorldInventory> inventories = new HashSet<WorldInventory>();

	public static Collection<WorldInventory> getAll() {
		return inventories;
	}

	public static void init(String filename) {
		FileConfiguration config = new FileConfiguration(filename);
		config.load();
		for (ConfigurationNode node : config.getNodes()) {
			List<String> worlds = node.getList("worlds", String.class);
			if (worlds.isEmpty()) {
				continue;
			}
			WorldInventory inv = new WorldInventory(node.get("folder", String.class, null));
			for (String world : worlds) {
				inv.add(world);
			}
		}
	}

	public static void deinit(String filename) {
		FileConfiguration config = new FileConfiguration(filename);
		int i = 0;
		for (WorldInventory inventory : inventories) {
			ConfigurationNode node = config.getNode("inv" + i++);
			node.set("folder", inventory.worldname);
			node.set("worlds", new ArrayList<String>(inventory.worlds));
		}
		config.save();
	}

	public static void detach(Collection<String> worldnames) {
		for (String world : worldnames) {
			WorldConfig.get(world).inventory.remove(world);
		}
	}

	public static void merge(Collection<String> worldnames) {
		WorldInventory inv = new WorldInventory();
		for (String world : worldnames) {
			inv.add(world);
		}
	}

	public WorldInventory() {
		inventories.add(this);
	}

	public WorldInventory(String worldFolder) {
		this();
		worldFolder = WorldManager.matchWorld(worldFolder);
		this.setFolder(worldFolder);
	}

	private final Set<String> worlds = new HashSet<String>();
	private File folder;
	private String worldname;
	private String lastSavedPlayer;

	public Collection<String> getWorlds() {
		return this.worlds;
	}

	public String getSharedWorldName() {
		return this.worldname;
	}

	public void save(World world, Player player) {
		if (world != null && player != null) {
			save(WorldUtil.getNative(world), EntityUtil.getNative(player));
		}
	}

	public void save(WorldServer world, EntityPlayer player) {
		if (world != null && player != null) {
			if (this.lastSavedPlayer != null && lastSavedPlayer.equalsIgnoreCase(player.name)) {
				return;
			}
			this.lastSavedPlayer = player.name;
			IDataManager man = world.getDataManager();
			if (man instanceof WorldNBTStorage) {
				((WorldNBTStorage) man).save(player);
			}
		}
	}

	public void resetSave() {
		this.lastSavedPlayer = null;
	}

	public WorldInventory remove(String worldname) {
		if (this.worlds.remove(worldname.toLowerCase())) {
			//constructor handles world config update
			new WorldInventory(worldname).add(worldname);
		}
		if (this.worlds.isEmpty()) {
			inventories.remove(this);
		}
		return this;
	}

	public WorldInventory add(String worldname) {
		WorldConfig config = WorldConfig.get(worldname);
		if (config.inventory != null) {
			config.inventory.remove(config.worldname);
		}
		config.inventory = this;
		this.worlds.add(worldname.toLowerCase());
		this.updateFolder(WorldManager.getWorld(worldname));
		return this;
	}

	public void updateFolder(WorldServer world) {
		if (this.updateFolder() && world != null) {
			playerField.set(world.getDataManager(), this.folder);
		}
	}

	public void updateFolder(World world) {
		if (world != null) {
			updateFolder(((CraftWorld) world).getHandle());
		}
	}

	private void setFolder(String worldname) {
		if (worldname == null) {
			this.worldname = null;
			this.folder = null;
		} else {
			this.worldname = worldname;
			this.folder = new File(Bukkit.getWorldContainer(), worldname);
			this.folder = new File(this.folder, "players");
		}
	}

	public boolean updateFolder() {
		if (this.folder == null || !this.folder.exists()) {
			this.refreshFolder();
			for (String worldname : this.worlds) {
				worldname = WorldManager.matchWorld(worldname);
				if (worldname != null) {
					this.setFolder(worldname);
				}
			}
			if (this.folder == null) {
				return false;
			} else {
				for (String worldname : this.worlds) {
					this.updateFolder(WorldManager.getWorld(worldname));
				}
			}
		}
		return true;
	}

	public void refreshFolder() {
		this.folder = null;
		this.worldname = null;
	}
}
