package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

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
	public static Collection<WorldConfig> all() {
		return config.values();
	}
	public static void loadAll(String filename) {
		Configuration config = new Configuration(filename);
		config.load();
		String[] worlds = WorldManager.getWorlds();
		for (String worldname : config.getKeys(false)) {
			for (String world : worlds) {
				if (world.equalsIgnoreCase(worldname)) {
					new WorldConfig(worldname, config);
					
					if (config.getBoolean(worldname + ".loaded", false)) {
						if (WorldManager.worldExists(worldname)) {
							if (WorldManager.getOrCreateWorld(worldname) == null) {
								MyWorlds.log(Level.SEVERE, "Failed to (pre)load world: " + worldname);
							}
						} else {
							MyWorlds.log(Level.WARNING, "World: " + worldname + " no longer exists and has not been loaded!");
						}
					}
					break;
				}
			}
		}
	}
	public static void saveAll(String filename) {
		Configuration config = new Configuration(new File(filename));
		for (String key : config.getKeys(false)) {
			config.set(key, null);
		}
		for (WorldConfig wc : all()) {
			wc.save(config);
		}
		config.save();
	}
	public static void remove(String worldname) {
		config.remove(worldname.toLowerCase());
	}
	
	public WorldConfig(String worldname, Configuration config) {
		this(worldname);
		worldname += ".";
		this.keepSpawnInMemory = config.getBoolean(worldname + "keepSpawnLoaded", this.keepSpawnInMemory);
		this.environment = Util.parseEnvironment(config.getString(worldname + "environment"), this.environment);
		this.chunkGeneratorName = config.getString(worldname + "chunkGenerator", null);
		this.difficulty = Util.parseDifficulty(config.getString(worldname + "difficulty"), this.difficulty);
		this.gameMode = Util.parseGameMode(config.getString(worldname + "gamemode", null), null);
		String worldspawn = config.getString(worldname + "spawn.world", null);
		if (worldspawn != null) {
			double x = config.getDouble(worldname + "spawn.x", 0);
			double y = config.getDouble(worldname + "spawn.y", 64);
			double z = config.getDouble(worldname + "spawn.z", 0);
			float yaw = (float) config.getDouble(worldname + "spawn.yaw", 0);
			float pitch = (float) config.getDouble(worldname + "spawn.pitch", 0);
			this.spawnPoint = new Position(worldspawn, x, y, z, yaw, pitch);
		}
		this.holdWeather = config.getBoolean(worldname + "holdWeather", false);
		this.pvp = config.getBoolean(worldname + "pvp", this.pvp);
		for (String type : config.getListOf(worldname + "deniedCreatures", new ArrayList<String>())) {
			type = type.toUpperCase();
			if (type.equals("ANIMALS")) {
				this.spawnControl.setAnimals(true);
			} else if (type.equals("MONSTERS")) {
				this.spawnControl.setMonsters(true);
			} else {
				try {
					this.spawnControl.deniedCreatures.add(CreatureType.valueOf(type));
				} catch (Exception ex) {}
			}
		}
    	long time = config.getInt(worldname + "lockedtime", Integer.MIN_VALUE);
    	if (time != Integer.MIN_VALUE) {
			this.timeControl.lockTime(time);
			this.timeControl.setLocking(true);
    	}
    	this.defaultPortal = config.getString(worldname + "defaultPortal", null);
    	this.OPlist = config.getListOf(worldname + "operators", this.OPlist);
	}
	public WorldConfig(String worldname) {
		this.worldname = worldname;
		worldname = worldname.toLowerCase();
		config.put(worldname, this);
		World world = this.getWorld();
		if (world != null) {
			this.keepSpawnInMemory = world.getKeepSpawnInMemory();
			this.environment = world.getEnvironment();
			this.difficulty = world.getDifficulty();
			this.spawnPoint = new Position(world.getSpawnLocation());
			this.pvp = world.getPVP();
			this.autosave = world.isAutoSave();
		} else {
			this.keepSpawnInMemory = true;
			this.environment = Util.parseEnvironment(worldname, Environment.NORMAL);
			this.difficulty = Difficulty.NORMAL;
			this.spawnPoint = new Position(worldname, 0, 64, 0);
			this.pvp = true;
		}
		this.spawnControl = new SpawnControl();
		this.timeControl = new TimeControl(this.worldname);
		for (OfflinePlayer op : Bukkit.getServer().getOperators()) {
			this.OPlist.add(op.getName());
		}
		this.gameMode = Bukkit.getServer().getDefaultGameMode();
	}
	
	public String worldname;
	public boolean keepSpawnInMemory;
	public Environment environment;
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
		if (this.gameMode != null) {
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
	public void update(World world) {
		if (world == null) return;
		updatePVP(world);
		updateKeepSpawnInMemory(world);
		updateDifficulty(world);
		updateAutoSave(world);
	}
	public void update(Player player) {
		updateOP(player);
		updateGamemode(player);
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
	
	public void save(Configuration config) {
		//Set if the world can be directly accessed
		World w = this.getWorld();
		if (w != null) {
	        this.difficulty = w.getDifficulty();
	        this.keepSpawnInMemory = w.getKeepSpawnInMemory();
	        this.autosave = w.isAutoSave();
		}
		
		String worldname = this.worldname + ".";
		config.set(worldname + "loaded", WorldManager.isLoaded(this.worldname));
		config.set(worldname + "keepSpawnLoaded", this.keepSpawnInMemory);
		config.set(worldname + "environment", this.environment.toString());
		config.set(worldname + "chunkGenerator", this.chunkGeneratorName);
		if (this.gameMode == null) {
			config.set(worldname + "gamemode", "NONE");
		} else {
			config.set(worldname + "gamemode", this.gameMode.toString());
		}
		
		if (this.timeControl.locker != null && this.timeControl.locker.isRunning()) {
			config.set(worldname + "lockedtime", this.timeControl.locker.time);
		} else {
			config.set(worldname + "lockedtime", null);
		}

		ArrayList<String> creatures = new ArrayList<String>();
		for (CreatureType type : this.spawnControl.deniedCreatures) {
			creatures.add(type.name());
		}
		config.set(worldname + "defaultPortal", this.defaultPortal);
	    config.set(worldname + "operators", this.OPlist);
		config.set(worldname + "deniedCreatures", creatures);
		config.set(worldname + "holdWeather", this.holdWeather);
		config.set(worldname + "difficulty", this.difficulty.toString());
		config.set(worldname + "spawn.world", this.spawnPoint.getWorldName());
		config.set(worldname + "spawn.x", this.spawnPoint.getX());
		config.set(worldname + "spawn.y", this.spawnPoint.getY());
		config.set(worldname + "spawn.z", this.spawnPoint.getZ());
	}
	
}
