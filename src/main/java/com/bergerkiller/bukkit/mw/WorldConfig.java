package com.bergerkiller.bukkit.mw;

import java.io.File;
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
import org.bukkit.World.Environment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.nbt.CommonTagCompound;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.bukkit.common.utils.MaterialUtil;
import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.mw.external.MultiverseHandler;
import com.bergerkiller.bukkit.mw.external.SpoutPluginHandler;

public class WorldConfig extends WorldConfigStore {
	public String worldname;
	public boolean keepSpawnInMemory = true;
	public WorldMode worldmode = WorldMode.NORMAL;
	private String chunkGeneratorName;
	public Difficulty difficulty = Difficulty.NORMAL;
	public Position spawnPoint;
	public GameMode gameMode = null;
	public boolean holdWeather = false;
	public boolean pvp = true;
	public final SpawnControl spawnControl = new SpawnControl();
	public final TimeControl timeControl = new TimeControl(this);
	private String defaultNetherPortal;
	private String defaultEnderPortal;
	public List<String> OPlist = new ArrayList<String>();
	public boolean allowHunger = true;
	public boolean autosave = true;
	public boolean reloadWhenEmpty = false;
	public boolean formSnow = true;
	public boolean formIce = true;
	public boolean showRain = true;
	public boolean showSnow = true;
	public boolean clearInventory = false;
	public boolean forcedRespawn = false;
	public boolean rememberLastPlayerPosition = false;
	public WorldInventory inventory;

	protected WorldConfig(String worldname) {
		this.worldname = worldname;
	}

	/**
	 * Resets all settings in this World Configuration to the defaults.
	 * This method acts as if the world is loaded again for the first time.
	 */
	public void reset() {
		// Try to import from other World Management plugins
		if (!MyWorlds.importFromMultiVerse || !MultiverseHandler.readWorldConfiguration(this)) {
			// Not imported, (try to) load from the default world configuration
			ConfigurationNode defaultsConfig = getDefaultProperties();
			if (defaultsConfig != null) {
				// Load using a clone to prevent altering the original
				this.load(defaultsConfig);
				if (defaultsConfig.contains(this.worldmode.getName())) {
					this.load(defaultsConfig.getNode(this.worldmode.getName()));
				}
			}
		}

		// Refresh
		World world = getWorld();
		if (world != null) {
			updateAll(world);
		}
	}

	/**
	 * Loads the default settings for a world.
	 * This method expects the world to be registered in the mapping prior.
	 */
	protected void loadDefaults() {
		World world = this.getWorld();
		if (world != null) {
			// Read from the loaded world directly
			this.keepSpawnInMemory = world.getKeepSpawnInMemory();
			this.worldmode = WorldMode.get(world);
			this.difficulty = world.getDifficulty();
			this.spawnPoint = new Position(world.getSpawnLocation());
			this.pvp = world.getPVP();
			this.autosave = world.isAutoSave();
			this.getChunkGeneratorName();
		} else {
			this.worldmode = WorldMode.get(worldname);
			this.spawnPoint = new Position(worldname, 0, 128, 0);
			if (WorldManager.worldExists(this.worldname)) {
				// Open up the level.dat of the World and read the settings from it
				CommonTagCompound data = WorldManager.getData(this.worldname);
				if (data != null) {
					// Read the settings from it
					this.spawnPoint.setX(data.getValue("SpawnX", this.spawnPoint.getBlockX()));
					this.spawnPoint.setY(data.getValue("SpawnY", this.spawnPoint.getBlockY()));
					this.spawnPoint.setZ(data.getValue("SpawnZ", this.spawnPoint.getBlockZ()));
				}
				// Figure out the world mode by inspecting the region files in the world
				// On failure, it will resort to using the world name to figure it out
				File regions = Common.SERVER.getWorldRegionFolder(this.worldname);
				if (regions != null && regions.isDirectory()) {
					// Skip the 'region' folder found in the dimension folder
					regions = regions.getParentFile();
					// Find out what name the current folder is
					// If this is DIM1 or DIM-1 then it is the_end/nether
					// Otherwise we will resort to using the world name
					String dimName = regions.getName();
					if (dimName.equals("DIM1")) {
						this.worldmode = WorldMode.THE_END;
					} else if (dimName.equals("DIM-1")) {
						this.worldmode = WorldMode.NETHER;
					}
				}
			}
		}
		if (MyWorlds.useWorldOperators) {
			for (OfflinePlayer op : Bukkit.getServer().getOperators()) {
				this.OPlist.add(op.getName());
			}
		}
		this.inventory = new WorldInventory(this.worldname).add(worldname);
	}
	
	/**
	 * Sets the generator name and arguments for this World.
	 * Note that this does not alter the generator for a possible loaded world.
	 * Only after re-loading does this take effect.
	 * 
	 * @param name to set to
	 */
	public void setChunkGeneratorName(String name) {
		this.chunkGeneratorName = name;
	}

	/**
	 * Gets the generator name and arguments of this World
	 * 
	 * @return Chunk Generator name and arguments
	 */
	public String getChunkGeneratorName() {
		if (this.chunkGeneratorName == null) {
			World world = this.getWorld();
			if (world != null) {
				ChunkGenerator gen = world.getGenerator();
				if (gen != null) {
					Plugin genPlugin = CommonUtil.getPluginByClass(gen.getClass());
					if (genPlugin != null) {
						this.chunkGeneratorName = genPlugin.getName();
					}
				}
			}
		}
		return this.chunkGeneratorName;
	}

	public void load(WorldConfig config) {
		this.keepSpawnInMemory = config.keepSpawnInMemory;
		this.worldmode = config.worldmode;
		this.chunkGeneratorName = config.chunkGeneratorName;
		this.difficulty = config.difficulty;
		this.spawnPoint = config.spawnPoint.clone();
		this.gameMode = config.gameMode;
		this.allowHunger = config.allowHunger;
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
		this.worldmode = WorldMode.get(node.get("environment", this.worldmode.getName()));
		this.chunkGeneratorName = node.get("chunkGenerator", String.class, this.chunkGeneratorName);
		if (LogicUtil.nullOrEmpty(this.chunkGeneratorName)) {
			this.chunkGeneratorName = null;
		}
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
		this.allowHunger = node.get("hunger", this.allowHunger);
		this.rememberLastPlayerPosition = node.get("rememberlastplayerpos", this.rememberLastPlayerPosition);
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
		this.defaultEnderPortal = node.get("defaultEndPortal", String.class, this.defaultEnderPortal);
    	if (node.contains("defaultPortal")) {
    		// Compatibility mode
    		this.defaultNetherPortal = node.get("defaultPortal", String.class, this.defaultNetherPortal);
    		node.set("defaultPortal", null);
    	}
    	this.OPlist = node.getList("operators", String.class, this.OPlist);
	}

	public void saveDefault(ConfigurationNode node) {
		save(node);
		// Remove nodes we rather not see
		node.remove("environment");
		node.remove("name");
		node.remove("chunkGenerator");
		node.remove("spawn");
		node.remove("loaded");
	}

	public void save(ConfigurationNode node) {
		//Set if the world can be directly accessed
		World w = this.getWorld();
		if (w != null) {
	        this.difficulty = w.getDifficulty();
	        this.keepSpawnInMemory = w.getKeepSpawnInMemory();
	        this.autosave = w.isAutoSave();
		}
		if (this.worldname == null || this.worldname.equals(this.getConfigName())) {
			node.remove("name");
		} else {
			node.set("name", this.worldname);
		}
		node.set("loaded", w != null);
		node.set("keepSpawnLoaded", this.keepSpawnInMemory);
		node.set("environment", this.worldmode.getName());
		node.set("chunkGenerator", LogicUtil.fixNull(this.getChunkGeneratorName(), ""));
		node.set("clearInventory", this.clearInventory ? true : null);
		node.set("gamemode", this.gameMode == null ? "NONE" : this.gameMode.toString());

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
		node.set("rememberlastplayerpos", this.rememberLastPlayerPosition);
		node.set("pvp", this.pvp);
		node.set("defaultNetherPortal", this.defaultNetherPortal);
		node.set("defaultEndPortal", this.defaultEnderPortal);
		node.set("operators", this.OPlist);
		node.set("deniedCreatures", creatures);
		node.set("holdWeather", this.holdWeather);
		node.set("hunger", this.allowHunger);
		node.set("formIce", this.formIce);
		node.set("formSnow", this.formSnow);
		node.set("showRain", this.showRain);
		node.set("showSnow", this.showSnow);
		node.set("difficulty", this.difficulty == null ? "NONE" : this.difficulty.toString());
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

	/**
	 * Regenerates the spawn point for a world if it is not properly set<br>
	 * Also updates the spawn position in the world configuration
	 * 
	 * @param world to regenerate the spawn point for
	 */
	public void fixSpawnLocation() {
		// Obtain the configuration and the set spawn position from it
		World world = spawnPoint.getWorld();
		if (world == null) {
			return;
		}

		Environment env = world.getEnvironment();
		if (env == Environment.NETHER || env == Environment.THE_END) {
			// Use a portal agent to generate the world spawn point
			Location loc = WorldUtil.findSpawnLocation(spawnPoint);
			if (loc == null) {
				return; // Failure?
			}
			spawnPoint = new Position(loc);
		} else {
			spawnPoint.setY(world.getHighestBlockYAt(spawnPoint));
		}

		// Minor offset
		spawnPoint.setX(0.5 + (double) spawnPoint.getBlockX());
		spawnPoint.setY(0.5 + (double) spawnPoint.getBlockY());
		spawnPoint.setZ(0.5 + (double) spawnPoint.getBlockZ());

		// Apply position to the world if same world
		if (!isOtherWorldSpawn()) {
			world.setSpawnLocation(spawnPoint.getBlockX(), spawnPoint.getBlockY(), spawnPoint.getBlockZ());
		}
	}
	
	public boolean isOtherWorldSpawn() {
		return !spawnPoint.getWorldName().equalsIgnoreCase(worldname);
	}

	/**
	 * Gets the name of the destination default nether portals teleport to on this World.
	 * If this returns null, then none is known at this time, indicating that it can be
	 * automatically set. If this returns an empty String, it indicates the user forcibly
	 * requested no destination and no automatic detection will be attempted.
	 * 
	 * @return Default nether portal destination name
	 */
	public String getNetherPortal() {
		return this.defaultNetherPortal;
	}

	public void setNetherPortal(String destination) {
		this.defaultNetherPortal = destination;
	}

	/**
	 * Gets the name of the destination default ender portals teleport to on this World.
	 * If this returns null, then none is known at this time, indicating that it can be
	 * automatically set. If this returns an empty String, it indicates the user forcibly
	 * requested no destination and no automatic detection will be attempted.
	 * 
	 * @return Default ender portal destination name
	 */
	public String getEnderPortal() {
		return this.defaultEnderPortal;
	}

	public void setEnderPortal(String destination) {
		this.defaultEnderPortal = destination;
	}

	/**
	 * Fired right before a player respawns.
	 * Note that no leave is fired after this event.
	 * It is a replacement for leaving a world (if that is the case)
	 * 
	 * @param player that respawns
	 */
	public void onRespawn(Player player, Location respawnLocation) {
		MyWorlds.plugin.getPlayerDataController().onRespawnSave(player, respawnLocation);
	}

	/**
	 * Fired when a player leaves this world
	 * 
	 * @param player that is about to leave this world
	 * @param quit state: True if the player left the server, False if not
	 */
	public void onPlayerLeave(Player player, boolean quit) {
		// If not quiting (it saves then anyhow!) save the old information
		if (!quit) {
			CommonUtil.savePlayer(player);
		}
	}

	/**
	 * Fired when a player left this world and already joined another world.
	 * If the old player position, inventory and etc. is important, add the logic in
	 * {@link #onPlayerLeave(Player, boolean)} instead.
	 * 
	 * @param player that left this world (may no longer contain valid information)
	 */
	public void onPlayerLeft(Player player) {
		this.updateReload();
	}

	/**
	 * Fired when a player joined this world
	 * 
	 * @param player that just entered this world
	 */
	public void onPlayerEnter(Player player) {
		// Apply world-specific settings
		updateOP(player);
		updateGamemode(player);
		updateSpoutWeather(player);
		updateHunger(player);
		// Refresh states based on the new world the player joined
		MWPlayerDataController.refreshState(player);
	}

	public void onWorldLoad(World world) {
		// Update settings
		updateAll(world);
		// Detect default portals
		tryCreatePortalLink();
	}

	public void onWorldUnload(World world) {
		// If the actual World spawnpoint changed, be sure to update accordingly
		if (!isOtherWorldSpawn()) {
			Location spawn = world.getSpawnLocation();
			if (spawnPoint.getBlockX() != spawn.getBlockX() || spawnPoint.getBlockY() != spawn.getBlockY() 
					|| spawnPoint.getBlockZ() != spawn.getBlockZ()) {
				spawnPoint = new Position(spawn);
			}
		}

		// Disable time control
		timeControl.updateWorld(null);
	}

	public World loadWorld() {
		if (WorldManager.worldExists(this.worldname)) {
			World w = WorldManager.getOrCreateWorld(this.worldname);
			if (w == null) {
				MyWorlds.plugin.log(Level.SEVERE, "Failed to (pre)load world: " + worldname);
			} else {
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

	/**
	 * Updates all components of this World Configuration to the loaded world specified
	 * 
	 * @param world to apply it to
	 */
	public void updateAll(World world) {
		// Fix spawn point if needed
		if (MaterialUtil.SUFFOCATES.get(this.spawnPoint.getBlock())) {
			this.fixSpawnLocation();
		} else if (!isOtherWorldSpawn()) {
			world.setSpawnLocation(this.spawnPoint.getBlockX(), this.spawnPoint.getBlockY(), this.spawnPoint.getBlockZ());
		}
		// Update world settings
		updatePVP(world);
		updateKeepSpawnInMemory(world);
		updateDifficulty(world);
		updateAutoSave(world);
		timeControl.updateWorld(world);
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
	public void updateHunger(Player player) {
		if (!allowHunger) {
			player.setFoodLevel(20);
		}
	}
	public void updateHunger(World world) {
		for (Player player : WorldUtil.getPlayers(world)) {
			updateHunger(player);
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

	/**
	 * Tries to create a portal link with another world currently available.
	 * 
	 * @see #tryCreatePortalLink(WorldConfig, WorldConfig)
	 */
	public void tryCreatePortalLink() {
		// Try to detect a possible portal link between worlds
		for (WorldConfig otherConfig : WorldConfig.all()) {
			if (otherConfig != this) {
				WorldConfig.tryCreatePortalLink(this, otherConfig);
			}
		}
	}

	/**
	 * Tries to create a default portal link if none is currently set between two worlds
	 * Only if the worlds match names and no portal is set will this create a link.
	 * 
	 * @param world1
	 * @param world1
	 */
	public static void tryCreatePortalLink(WorldConfig world1, WorldConfig world2) {
		// Name compatibility check
		if (!world1.getRawWorldName().equals(world2.getRawWorldName())) {
			return;
		}
		// If same environment, directly stop it here
		if (world1.worldmode == world2.worldmode) {
			return;
		}
		// Default nether portal check and detection
		if (world1.defaultNetherPortal == null && world2.defaultEnderPortal == null) {
			// Try to create a link with the nether portals
			if ((world1.worldmode == WorldMode.NETHER && world2.worldmode == WorldMode.NORMAL) ||
					(world2.worldmode == WorldMode.NETHER && world1.worldmode == WorldMode.NORMAL)) {
				// Nether link detected!
				world1.defaultNetherPortal = world2.worldname;
				world2.defaultNetherPortal = world1.worldname;
				MyWorlds.plugin.log(Level.INFO, "Created nether portal link between worlds '" + world1.worldname + "' and '" + world2.worldname + "'!");
			}
		}
		// Default ender portal check and detection
		if (world1.defaultEnderPortal == null && world2.defaultEnderPortal == null) {
			// Try to create a link with the nether portals
			if ((world1.worldmode == WorldMode.THE_END && world2.worldmode == WorldMode.NORMAL) ||
					(world2.worldmode == WorldMode.THE_END && world1.worldmode == WorldMode.NORMAL)) {
				// Nether link detected!
				world1.defaultEnderPortal = world2.worldname;
				world2.defaultEnderPortal = world1.worldname;
				MyWorlds.plugin.log(Level.INFO, "Created ender portal link between worlds '" + world1.worldname + "' and '" + world2.worldname + "'!");
			}
		}
	}

	/**
	 * Gets the World Name of this World excluding any _nether, _the_end or DIM extensions from the name.
	 * Returns an empty String if the world name equals DIM1 or DIM-1 (for MCPC+ server).
	 * In all cases, a lower-cased world name is returned.
	 * 
	 * @return Raw world name
	 */
	private String getRawWorldName() {
		String lower = this.worldname.toLowerCase();
		if (lower.endsWith("_nether")) {
			return lower.substring(0, lower.length() - 7);
		}
		if (lower.endsWith("_the_end")) {
			return lower.substring(0, lower.length() - 8);
		}
		if (lower.equals("dim1") || lower.equals("dim-1")) {
			return "";
		}
		return lower;
	}
}
