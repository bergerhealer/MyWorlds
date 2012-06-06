package com.bergerkiller.bukkit.mw;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import net.minecraft.server.EntityPlayer;
import net.minecraft.server.IDataManager;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.WorldNBTStorage;
import net.minecraft.server.WorldServer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.block.SpoutWeather;
import org.getspout.spoutapi.player.SpoutPlayer;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.config.FileConfiguration;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.EnumUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

public class WorldConfig {	
	private static HashMap<String, WorldConfig> config = new HashMap<String, WorldConfig>();
	public static WorldConfig get(String worldname) {
		WorldConfig c = config.get(worldname.toLowerCase());
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
		return config.values();
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
		config.clear();
		config = null;
	}

	public static void remove(String worldname) {
		config.remove(worldname.toLowerCase());
	}

	public WorldConfig(ConfigurationNode node) {
		this(node.getName());
		this.keepSpawnInMemory = node.get("keepSpawnLoaded", this.keepSpawnInMemory);
		this.worldmode = EnumUtil.parse(node.get("environment", String.class), this.worldmode);
		this.chunkGeneratorName = node.get("chunkGenerator", String.class);
		this.difficulty = EnumUtil.parseDifficulty(node.get("difficulty", String.class), this.difficulty);
		this.gameMode = EnumUtil.parseGameMode(node.get("gamemode", String.class), null);
		String worldspawn = node.get("spawn.world", String.class);
		if (worldspawn != null) {
			double x = node.get("spawn.x", 0.0);
			double y = node.get("spawn.y", 64.0);
			double z = node.get("spawn.z", 0.0);
			double yaw = node.get("spawn.yaw", 0.0);
			double pitch = node.get("spawn.pitch", 0.0);
			this.spawnPoint = new Position(worldspawn, x, y, z, (float) yaw, (float) pitch);
		}
		this.holdWeather = node.get("holdWeather", false);
		this.formIce = node.get("formIce", true);
		this.formSnow = node.get("formSnow", true);
		this.showRain = node.get("showRain", true);
		this.showSnow = node.get("showSnow", true);
		this.pvp = node.get("pvp", this.pvp);
		this.reloadWhenEmpty = node.get("reloadWhenEmpty", this.reloadWhenEmpty);
		for (String type : node.getList("deniedCreatures", String.class)) {
			type = type.toUpperCase();
			if (type.equals("ANIMALS")) {
				this.spawnControl.setAnimals(true);
			} else if (type.equals("MONSTERS")) {
				this.spawnControl.setMonsters(true);
			} else {
				EntityType t = EnumUtil.parse(EntityType.class, type, null);
				if (t != null) {
					this.spawnControl.deniedCreatures.add(t);
				}
			}
		}
    	long time = node.get("lockedtime", Integer.MIN_VALUE);
    	if (time != Integer.MIN_VALUE) {
			this.timeControl.setTime(time);
			this.timeControl.setLocking(true);
    	}
    	this.defaultPortal = node.get("defaultPortal", String.class);
    	this.OPlist = node.getList("operators", String.class, this.OPlist);
	}
	public WorldConfig(String worldname) {
		this.worldname = worldname;
		worldname = worldname.toLowerCase();
		config.put(worldname, this);
		World world = this.getWorld();
		if (world != null) {
			this.keepSpawnInMemory = world.getKeepSpawnInMemory();
			this.worldmode = WorldMode.get(world);
			this.difficulty = world.getDifficulty();
			this.spawnPoint = new Position(world.getSpawnLocation());
			this.pvp = world.getPVP();
			this.autosave = world.isAutoSave();
		} else {
			this.keepSpawnInMemory = true;
			this.worldmode = WorldMode.get(worldname);
			this.difficulty = Difficulty.NORMAL;
			this.spawnPoint = new Position(worldname, 0, 64, 0);
			this.pvp = true;
		}
		this.spawnControl = new SpawnControl();
		this.timeControl = new TimeControl(this);
		this.inventory = new WorldInventory(this.worldname).add(worldname);
		if (MyWorlds.useWorldOperators) {
			for (OfflinePlayer op : Bukkit.getServer().getOperators()) {
				this.OPlist.add(op.getName());
			}
		}
		this.gameMode = Bukkit.getServer().getDefaultGameMode();
	}
	public void save(ConfigurationNode node) {
		//Set if the world can be directly accessed
		World w = this.getWorld();
		if (w != null) {
	        this.difficulty = w.getDifficulty();
	        this.keepSpawnInMemory = w.getKeepSpawnInMemory();
	        this.autosave = w.isAutoSave();
	        if (this.chunkGeneratorName == null) {
	        	ChunkGenerator gen = ((org.bukkit.craftbukkit.CraftWorld) w).getHandle().generator;
	        	if (gen != null) {
	        		String name = gen.getClass().getName();
	        		if (name.equals("bukkit.techguard.christmas.world.ChristmasGenerator")) {
	        			this.chunkGeneratorName = "Christmas";
	        		}
	        	}
	        }
		}
		
		node.set("loaded", w != null);
		node.set("keepSpawnLoaded", this.keepSpawnInMemory);
		node.set("environment", this.worldmode.toString());
		node.set("chunkGenerator", this.chunkGeneratorName);
		if (this.gameMode == null) {
			node.set("gamemode", "NONE");
		} else {
			node.set("gamemode", this.gameMode.toString());
		}
		
		if (this.timeControl.isLocked()) {
			node.set("lockedtime", this.timeControl.getTime());
		} else {
			node.remove("lockedtime");
		}

		ArrayList<String> creatures = new ArrayList<String>();
		for (EntityType type : this.spawnControl.deniedCreatures) {
			creatures.add(type.name());
		}
		node.set("pvp", this.pvp);
		node.set("defaultPortal", this.defaultPortal);
		node.set("operators", this.OPlist);
		node.set("deniedCreatures", creatures);
		node.set("holdWeather", this.holdWeather);
		node.set("formIce", this.formIce);
		node.set("formSnow", this.formSnow);
		node.set("showRain", this.showRain);
		node.set("showSnow", this.showSnow);
		node.set("difficulty", this.difficulty.toString());
		node.set("reloadWhenEmpty", this.reloadWhenEmpty);
		node.set("spawn.world", this.spawnPoint.getWorldName());
		node.set("spawn.x", this.spawnPoint.getX());
		node.set("spawn.y", this.spawnPoint.getY());
		node.set("spawn.z", this.spawnPoint.getZ());
		node.set("spawn.yaw", (double) this.spawnPoint.getYaw());
		node.set("spawn.pitch", (double) this.spawnPoint.getPitch());
	}
	
	public String worldname;
	public boolean keepSpawnInMemory;
	public WorldMode worldmode;
	public String chunkGeneratorName;
	public Difficulty difficulty;
	public Position spawnPoint;
	public GameMode gameMode;
	public boolean holdWeather = false;
	public boolean pvp = false;
	public SpawnControl spawnControl;
	public TimeControl timeControl;
	public String defaultPortal;
	public List<String> OPlist = new ArrayList<String>();
	public boolean autosave = true;
	public boolean reloadWhenEmpty = false;
	public boolean formSnow = true;
	public boolean formIce = true;
	public boolean showRain = true;
	public boolean showSnow = true;
	public WorldInventory inventory;
	
	public World loadWorld() {
		if (WorldManager.worldExists(this.worldname)) {
			World w = WorldManager.getOrCreateWorld(this.worldname);
			if (w == null) {
				MyWorlds.plugin.log(Level.SEVERE, "Failed to (pre)load world: " + worldname);
			}
			return w;
		} else {
			MyWorlds.plugin.log(Level.WARNING, "World: " + worldname + " could not be loaded because it no longer exists!");
		}
		return null;
	}
	public boolean unloadWorld() {
		return WorldManager.unload(this.getWorld());
	}
	
	public static void updateReload(Player player) {
		updateReload(player.getWorld());
	}
	public static void updateReload(Location loc) {
		updateReload(loc.getWorld());
	}
	public static void updateReload(World world) {
		updateReload(world.getName());
	}
	public static void updateReload(String worldname) {
		new Task(MyWorlds.plugin, worldname) {
			public void run() {
				get(arg(0, String.class)).updateReload();
			}
		}.start(1);
	}
	public void updateSpoutWeather(World world) {
		if (!MyWorlds.isSpoutEnabled) return;
		for (Player p : world.getPlayers()) updateSpoutWeather(p);
	}
	public void updateSpoutWeather(Player player) {
		if (MyWorlds.isSpoutEnabled) {
			try {
				SpoutPlayer sp = SpoutManager.getPlayer(player);
				if (sp.isSpoutCraftEnabled()) {
					SpoutWeather w = SpoutWeather.NONE;
					if (player.getWorld().hasStorm()) {
						if (this.showRain && this.showSnow) {
							w = SpoutWeather.RESET;
						} else if (this.showRain) {
							w = SpoutWeather.RAIN;
						} else if (this.showSnow) {
							w = SpoutWeather.SNOW;
						}
					}
					SpoutManager.getBiomeManager().setPlayerWeather(SpoutManager.getPlayer(player), w);
				}
			} catch (Throwable t) {
				MyWorlds.isSpoutEnabled = false;
				MyWorlds.plugin.log(Level.SEVERE, "An error occured while using Spout, Spout is no longer used in MyWorlds from now on:");
				t.printStackTrace();
			}
		}
	}
	public void updateReload() {
		World world = this.getWorld();
		if (world == null) return;
		if (!this.reloadWhenEmpty) return;
		if (world.getPlayers().size() > 0) return;
		//reload world
		MyWorlds.plugin.log(Level.INFO, "Reloading world '" + worldname + "' - world became empty");
		if (!this.unloadWorld()) {
			MyWorlds.plugin.log(Level.WARNING, "Failed to unload world: " + worldname + " for reload purposes");
		} else if (this.loadWorld() == null) {
			MyWorlds.plugin.log(Level.WARNING, "Failed to load world: " + worldname + " for reload purposes");
		} else {
			MyWorlds.plugin.log(Level.INFO, "World reloaded successfully");
		}
	}
	public void updateAutoSave(World world) {
		if (world != null && world.isAutoSave() != this.autosave) {
			world.setAutoSave(this.autosave);
		}
	}
	public void updateOP(Player player) {
		if (MyWorlds.useWorldOperators) {
			boolean op = this.isOP(player);
			if (op != player.isOp()) {
				player.setOp(op);
				if (op) {
					player.sendMessage(ChatColor.YELLOW + "You are now op!");
				} else {
					player.sendMessage(ChatColor.RED + "You are no longer op!");
				}
			}
		}
	}
	public void updateOP(World world) {
		if (MyWorlds.useWorldOperators) {
			for (Player p : world.getPlayers()) updateOP(p);
		}
	}
	public void updateGamemode(Player player) {
		if (this.gameMode != null && !Permission.has(player, "world.ignoregamemode")) {
			player.setGameMode(this.gameMode);
		}
	}
	public void updatePVP(World world) {
		if (world != null && this.pvp != world.getPVP()) {
			world.setPVP(this.pvp);
		}
	}
	public void updateKeepSpawnInMemory(World world) { 
		if (world != null && world.getKeepSpawnInMemory() != this.keepSpawnInMemory) {
			world.setKeepSpawnInMemory(this.keepSpawnInMemory);
		}
	}
	public void updateDifficulty(World world) {
		if (world != null && world.getDifficulty() != this.difficulty) {
			world.setDifficulty(this.difficulty);
		}
	}
	public void updateInventory(final WorldServer world, final Player bukkitPlayer) {
		if (Permission.GENERAL_KEEPINV.has(bukkitPlayer)) {
			return;
		}
		EntityPlayer player = EntityUtil.getNative(bukkitPlayer);
		if (world != null && player != null) {
			IDataManager man = world.getDataManager();
			if (man instanceof WorldNBTStorage) {
				NBTTagCompound data = ((WorldNBTStorage) man).getPlayerData(player.name);
				if (data != null) {
					player.inventory.b(data.getList("Inventory"));
				}
			}
		}
	}
	public void update(World world) {
		if (world == null) return;
		updatePVP(world);
		updateKeepSpawnInMemory(world);
		updateDifficulty(world);
		updateAutoSave(world);
	}
	public void update(Player player) {
		this.inventory.resetSave();
		updateOP(player);
		updateGamemode(player);
		updateSpoutWeather(player);
		updateInventory(WorldUtil.getNative(this.getWorld()), player);
	}

	public void remove(Player player) {
		this.inventory.save(this.getWorld(), player);
	}

	public World getWorld() {
		return WorldManager.getWorld(this.worldname);
	}
	
	public boolean isOP(Player player) {
		for (String playername : OPlist) {
			if (playername.equals("\\*")) return true;
			if (player.getName().equalsIgnoreCase(playername)) return true;
		}
		return false;
	}
	public void setGameMode(GameMode mode) {
		if (this.gameMode != mode) {
			this.gameMode = mode;
			if (mode != null) {
				World world = this.getWorld();
				if (world != null) {
					for (Player p : world.getPlayers()) {
						this.updateGamemode(p);
					}
				}
			}
		}
	}

}
