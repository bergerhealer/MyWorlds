package com.bergerkiller.bukkit.mw;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.MaterialUtil;
import com.bergerkiller.bukkit.common.utils.ParseUtil;

public class WorldConfig extends WorldConfigStore {
	public String worldname;
	public boolean keepSpawnInMemory = true;
	public WorldMode worldmode;
	public String chunkGeneratorName;
	public Difficulty difficulty = Difficulty.NORMAL;
	public Position spawnPoint;
	public GameMode gameMode = null;
	public boolean holdWeather = false;
	public boolean pvp = true;
	public final SpawnControl spawnControl = new SpawnControl();
	public final TimeControl timeControl = new TimeControl(this);
	private String defaultNetherPortal;
	private String defaultEndPortal;
	public List<String> OPlist = new ArrayList<String>();
	public boolean autosave = true;
	public boolean reloadWhenEmpty = false;
	public boolean formSnow = true;
	public boolean formIce = true;
	public boolean showRain = true;
	public boolean showSnow = true;
	public boolean clearInventory = false;
	public boolean forcedRespawn = false;
	public WorldInventory inventory;

	public WorldConfig(String worldname) {
		this.worldname = worldname;
		if (worldname == null) {
			return;
		}
		worldname = worldname.toLowerCase();
		worldConfigs.put(worldname, this);
		World world = this.getWorld();
		if (world != null) {
			this.keepSpawnInMemory = world.getKeepSpawnInMemory();
			this.worldmode = WorldMode.get(world);
			this.difficulty = world.getDifficulty();
			this.spawnPoint = new Position(world.getSpawnLocation());
			this.pvp = world.getPVP();
			this.autosave = world.isAutoSave();

			// Obtain the chunk generator of this world
			// Note that we are unable to obtain the chunk generator arguments
			// This method will at least somewhat avoid chunk generator mishaps
			ChunkGenerator gen = world.getGenerator();
			if (gen != null) {
				Plugin genPlugin = CommonUtil.getPluginByClass(gen.getClass());
				if (genPlugin != null) {
					this.chunkGeneratorName = genPlugin.getName();
				}
			}
		} else {
			this.worldmode = WorldMode.get(worldname);
			this.spawnPoint = new Position(worldname, 0, 128, 0);
		}
		if (MyWorlds.useWorldOperators) {
			for (OfflinePlayer op : Bukkit.getServer().getOperators()) {
				this.OPlist.add(op.getName());
			}
		}
		this.inventory = new WorldInventory(this.worldname).add(worldname);
	}

	/**
	 * Handles the case of this configuration being made for a new world
	 */
	public void loadNew() {
		this.gameMode = Bukkit.getDefaultGameMode();
	}

	public void load(WorldConfig config) {
		this.keepSpawnInMemory = config.keepSpawnInMemory;
		this.worldmode = config.worldmode;
		this.chunkGeneratorName = config.chunkGeneratorName;
		this.difficulty = config.difficulty;
		this.spawnPoint = new Position(config.spawnPoint);
		this.gameMode = config.gameMode;
		this.holdWeather = config.holdWeather;
		this.pvp = config.pvp;
		this.spawnControl.deniedCreatures.clear();
		this.spawnControl.deniedCreatures.addAll(config.spawnControl.deniedCreatures);
		this.timeControl.setLocking(config.timeControl.isLocked());
		this.timeControl.setTime(timeControl.getTime());
		this.autosave = config.autosave;
		this.reloadWhenEmpty = config.reloadWhenEmpty;
		this.formSnow = config.formSnow;
		this.formIce = config.formIce;
		this.showRain = config.showRain;
		this.showSnow = config.showSnow;
		this.clearInventory = config.clearInventory;
		this.forcedRespawn = config.forcedRespawn;
		this.inventory = config.inventory.add(this.worldname);
	}

	public void load(ConfigurationNode node) {
		this.keepSpawnInMemory = node.get("keepSpawnLoaded", this.keepSpawnInMemory);
		this.worldmode = node.get("environment", this.worldmode);
		this.chunkGeneratorName = node.get("chunkGenerator", String.class, this.chunkGeneratorName);
		this.difficulty = node.get("difficulty", Difficulty.class, this.difficulty);
		this.gameMode = node.get("gamemode", GameMode.class, this.gameMode);
		this.clearInventory = node.get("clearInventory", this.clearInventory);
		String worldspawn = node.get("spawn.world", String.class);
		if (worldspawn != null) {
			double x = node.get("spawn.x", 0.0);
			double y = node.get("spawn.y", 64.0);
			double z = node.get("spawn.z", 0.0);
			double yaw = node.get("spawn.yaw", 0.0);
			double pitch = node.get("spawn.pitch", 0.0);
			this.spawnPoint = new Position(worldspawn, x, y, z, (float) yaw, (float) pitch);
		}
		this.holdWeather = node.get("holdWeather", this.holdWeather);
		this.formIce = node.get("formIce", this.formIce);
		this.formSnow = node.get("formSnow", this.formSnow);
		this.showRain = node.get("showRain", this.showRain);
		this.showSnow = node.get("showSnow", this.showSnow);
		this.pvp = node.get("pvp", this.pvp);
		this.forcedRespawn = node.get("forcedRespawn", this.forcedRespawn);
		this.reloadWhenEmpty = node.get("reloadWhenEmpty", this.reloadWhenEmpty);
		for (String type : node.getList("deniedCreatures", String.class)) {
			type = type.toUpperCase();
			if (type.equals("ANIMALS")) {
				this.spawnControl.setAnimals(true);
			} else if (type.equals("MONSTERS")) {
				this.spawnControl.setMonsters(true);
			} else {
				EntityType t = ParseUtil.parseEnum(EntityType.class, type, null);
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
		this.defaultNetherPortal = node.get("defaultNetherPortal", String.class, this.defaultNetherPortal);
		this.defaultEndPortal = node.get("defaultEndPortal", String.class, this.defaultEndPortal);
    	if (node.contains("defaultPortal")) {
    		// Compatibility mode
    		this.defaultNetherPortal = node.get("defaultPortal", String.class, this.defaultNetherPortal);
    		node.set("defaultPortal", null);
    	}
    	this.OPlist = node.getList("operators", String.class, this.OPlist);
	}

	public void save(ConfigurationNode node) {
		//Set if the world can be directly accessed
		World w = this.getWorld();
		if (w != null) {
	        this.difficulty = w.getDifficulty();
	        this.keepSpawnInMemory = w.getKeepSpawnInMemory();
	        this.autosave = w.isAutoSave();
	        if (this.chunkGeneratorName == null) {
	        	ChunkGenerator gen = w.getGenerator();
	        	if (gen != null) {
	        		String name = gen.getClass().getName();
	        		if (name.equals("bukkit.techguard.christmas.world.ChristmasGenerator")) {
	        			this.chunkGeneratorName = "Christmas";
	        		}
	        	}
	        }
		}
		if (this.worldname == null || this.worldname.equals(this.getConfigName())) {
			node.remove("name");
		} else {
			node.set("name", this.worldname);
		}
		node.set("loaded", w != null);
		node.set("keepSpawnLoaded", this.keepSpawnInMemory);
		node.set("environment", this.worldmode);
		node.set("chunkGenerator", this.chunkGeneratorName);
		node.set("clearInventory", this.clearInventory ? true : null);
		node.set("gamemode", this.gameMode);

		if (this.timeControl.isLocked()) {
			node.set("lockedtime", this.timeControl.getTime());
		} else {
			node.remove("lockedtime");
		}

		ArrayList<String> creatures = new ArrayList<String>();
		for (EntityType type : this.spawnControl.deniedCreatures) {
			creatures.add(type.name());
		}
		node.set("forcedRespawn", this.forcedRespawn);
		node.set("pvp", this.pvp);
		node.set("defaultNetherPortal", this.defaultNetherPortal);
		node.set("defaultEndPortal", this.defaultEndPortal);
		node.set("operators", this.OPlist);
		node.set("deniedCreatures", creatures);
		node.set("holdWeather", this.holdWeather);
		node.set("formIce", this.formIce);
		node.set("formSnow", this.formSnow);
		node.set("showRain", this.showRain);
		node.set("showSnow", this.showSnow);
		node.set("difficulty", this.difficulty.toString());
		node.set("reloadWhenEmpty", this.reloadWhenEmpty);
		if (this.spawnPoint == null) {
			node.remove("spawn");
		} else {
			node.set("spawn.world", this.spawnPoint.getWorldName());
			node.set("spawn.x", this.spawnPoint.getX());
			node.set("spawn.y", this.spawnPoint.getY());
			node.set("spawn.z", this.spawnPoint.getZ());
			node.set("spawn.yaw", (double) this.spawnPoint.getYaw());
			node.set("spawn.pitch", (double) this.spawnPoint.getPitch());
		}
	}

	public void setNetherPortal(String destination) {
		this.defaultNetherPortal = destination;
	}

	public void setEndPortal(String destination) {
		this.defaultEndPortal = destination;
	}

	public String getNetherPortal() {
		if (this.defaultNetherPortal == null) {
			if (this.worldmode == WorldMode.NETHER && this.worldname.toLowerCase().endsWith("_nether")) {
				this.defaultNetherPortal = this.worldname.substring(0, this.worldname.length() - 7);
			} else if (this.worldmode == WorldMode.THE_END && this.worldname.toLowerCase().endsWith("_the_end")) {
				this.defaultNetherPortal = this.worldname.substring(0, this.worldname.length() - 8) + "_nether";
			} else {
				this.defaultNetherPortal = this.worldname + "_nether";
			}
		}
		return this.defaultNetherPortal;
	}

	public String getEndPortal() {
		if (this.defaultEndPortal == null) {
			if (this.worldmode == WorldMode.NETHER && this.worldname.toLowerCase().endsWith("_nether")) {
				this.defaultEndPortal = this.worldname.substring(0, this.worldname.length() - 7) + "_the_end";
			} else if (this.worldmode == WorldMode.THE_END && this.worldname.toLowerCase().endsWith("_the_end")) {
				this.defaultEndPortal = this.worldname.substring(0, this.worldname.length() - 8);
			} else {
				this.defaultEndPortal = this.worldname + "_the_end";
			}
		}
		return this.defaultEndPortal;
	}


	public World loadWorld() {
		if (WorldManager.worldExists(this.worldname)) {
			World w = WorldManager.getOrCreateWorld(this.worldname);
			if (w == null) {
				MyWorlds.plugin.log(Level.SEVERE, "Failed to (pre)load world: " + worldname);
			} else {
				this.update(w);
				return w;
			}
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
	public static void updateReload(final String worldname) {
		CommonUtil.nextTick(new Runnable() {
			public void run() {
				get(worldname).updateReload();
			}
		});
	}
	public void updateSpoutWeather(World world) {
		if (!MyWorlds.isSpoutPluginEnabled) return;
		for (Player p : world.getPlayers()) updateSpoutWeather(p);
	}
	public void updateSpoutWeather(Player player) {
		if (MyWorlds.isSpoutPluginEnabled) {
			SpoutPluginHandler.setPlayerWeather(player, showRain, showSnow);
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
		if (this.gameMode != null && !Permission.GENERAL_IGNOREGM.has(player)) {
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
		if (MaterialUtil.SUFFOCATES.get(world.getBlockAt(this.spawnPoint))) {
			WorldManager.fixSpawnLocation(world);
		}
		updatePVP(world);
		updateKeepSpawnInMemory(world);
		updateDifficulty(world);
		updateAutoSave(world);
	}
	public void update(Player player) {
		updateOP(player);
		updateGamemode(player);
		updateSpoutWeather(player);
	}

	/**
	 * Gets a safe configuration name for this World Configuration<br>
	 * Unsafe characters, such as dots, are replaced
	 * 
	 * @return Safe config world name
	 */
	public String getConfigName() {
		if (this.worldname == null) {
			return "";
		}
		return this.worldname.replace('.', '_').replace(':', '_');
	}

	/**
	 * Gets the loaded World of this world configuration<br>
	 * If the world is not loaded, null is returned
	 * 
	 * @return the World
	 */
	public World getWorld() {
		return this.worldname == null ? null : WorldManager.getWorld(this.worldname);
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
