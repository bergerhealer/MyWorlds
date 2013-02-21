package com.bergerkiller.bukkit.mw;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import com.bergerkiller.bukkit.common.config.FileConfiguration;
import com.bergerkiller.bukkit.common.metrics.AddonHandler;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.PluginBase;

public class MyWorlds extends PluginBase {
	public static int teleportInterval;
	public static boolean useWaterTeleport;
	public static int timeLockInterval;
	public static boolean useWorldEnterPermissions;
	public static boolean usePortalEnterPermissions;
	public static boolean useWorldBuildPermissions;
	public static boolean useWorldUsePermissions;
	public static boolean useWorldChatPermissions;
	public static boolean allowPortalNameOverride;
	public static boolean useWorldOperators;
	public static boolean onlyObsidianPortals = false;
	public static boolean isSpoutPluginEnabled = false;
	public static boolean onlyPlayerTeleportation = true;
	public static boolean useWorldInventories;
	public static boolean calculateWorldSize;
	private static String mainWorld;
	public static boolean forceMainWorldSpawn;
	public static boolean alwaysInstantPortal;
	public static boolean allowPersonalPortals;
	public static MyWorlds plugin;

	@Override
	public int getMinimumLibVersion() {
		return Common.VERSION;
	}

	public String root() {
		return getDataFolder() + File.separator;
	}

	@Override
	public void updateDependency(Plugin plugin, String pluginName, boolean enabled) {
		if (pluginName.equals("Spout")) {
			isSpoutPluginEnabled = enabled;
		}
	}

	@Override
	public void enable() {
		plugin = this;

		// Event registering
		this.register(MWListener.class);
		this.register(MWPermissionListener.class);
		this.register("tpp", "world");

		FileConfiguration config = new FileConfiguration(this);
		config.load();

		config.setHeader("This is the configuration of MyWorlds");
		config.addHeader("For more information, you can visit the following websites:");
		config.addHeader("http://dev.bukkit.org/server-mods/my-worlds/");
		config.addHeader("http://forums.bukkit.org/threads/myworlds.31718");
		config.addHeader("http://wiki.bukkit.org/MyWorlds-Plugin");

		config.setHeader("teleportInterval", "\nThe interval in miliseconds a player has to wait before being teleported again");
		teleportInterval = config.get("teleportInterval", 2000);

		config.setHeader("useWaterTeleport", "\nWhether water stream portals are allowed");
		useWaterTeleport = config.get("useWaterTeleport", true);

		config.setHeader("timeLockInterval", "\nThe tick interval at which time is kept locked");
		timeLockInterval = config.get("timeLockInterval", 20);

		config.setHeader("useWorldInventories", "\nWhether or not world inventories are being separated using the settings");
		useWorldInventories = config.get("useWorldInventories", false);

		useWorldEnterPermissions = config.get("useWorldEnterPermissions", false);
		usePortalEnterPermissions = config.get("usePortalEnterPermissions", false);
		useWorldBuildPermissions = config.get("useWorldBuildPermissions", false);
		useWorldUsePermissions = config.get("useWorldUsePermissions", false);
		useWorldChatPermissions = config.get("useWorldChatPermissions", false);

		config.setHeader("onlyPlayerTeleportation", "\nWhether only players are allowed to teleport through portals");
		onlyPlayerTeleportation = config.get("onlyPlayerTeleportation", true);

		config.setHeader("allowPortalNameOverride", "\nWhether portals can be replaced by other portals with the same name on the same world");
		allowPortalNameOverride = config.get("allowPortalNameOverride", true);

		config.setHeader("useWorldOperators", "\nWhether each world has it's own operator list");
		useWorldOperators = config.get("useWorldOperators", false);

		config.setHeader("onlyObsidianPortals", "\nWhether only portal blocks surrounded by obsidian can teleport players");
		onlyObsidianPortals = config.get("onlyObsidianPortals", false);

		config.setHeader("calculateWorldSize", "\nWhether the world info command will calculate the world size on disk");
		config.addHeader("calculateWorldSize", "If this process takes too long, disable it to prevent possible server freezes");
		calculateWorldSize = config.get("calculateWorldSize", true);

		config.setHeader("mainWorld", "\nThe main world in which new players spawn");
		config.addHeader("mainWorld", "If left empty, the main world defined in the server.properties is used");
		mainWorld = config.get("mainWorld", "");

		config.setHeader("forceMainWorldSpawn", "\nWhether all players respawn on the main world at all times");
		forceMainWorldSpawn = config.get("forceMainWorldSpawn", false);

		config.setHeader("alwaysInstantPortal", "\nWhether survival players instantly teleport when entering a nether portal");
		alwaysInstantPortal = config.get("alwaysInstantPortal", true);

		config.setHeader("allowPersonalPortals", "\nWhether individually placed nether/end portals create their own destination portal");
		config.addHeader("allowPersonalPortals", "False: Players are teleported to the spawn point of the world");
		config.addHeader("allowPersonalPortals", "True: Players are teleported to their own portal on the other world");
		allowPersonalPortals = config.get("allowPersonalPortals", true);

		config.save();

		// World configurations have to be loaded first
		WorldConfig.init();

		// Portals
		Portal.init(this.getDataFile("portals.txt"));

		// World inventories
		WorldInventory.load();
		MWPlayerFileData.init();

		// init chunk loader
		LoadChunksTask.init();
		
		//metrics
		AddonHandler ah = new AddonHandler(this);
		ah.startMetrics();
	}

	@Override
	public void disable() {
		// Portals
		Portal.deinit(this.getDataFile("portals.txt"));

		// World inventories
		WorldInventory.save();

		// World configurations have to be cleared last
		WorldConfig.deinit();

		// Abort chunk loader
		LoadChunksTask.deinit();

		plugin = null;
	}

	@Override
	public boolean command(CommandSender sender, String cmdLabel, String[] args) {
		com.bergerkiller.bukkit.mw.commands.Command.execute(sender, cmdLabel, args);
		return true;
	}

	@Override
	public void localization() {
		this.loadLocales(Localization.class);
	}

	@Override
	public void permissions() {
		this.loadPermissions(Permission.class);
	}

	/**
	 * Gets the main world
	 * 
	 * @return Main world
	 */
	public static World getMainWorld() {
		if (!mainWorld.isEmpty()) {
			World world = Bukkit.getWorld(mainWorld);
			if (world != null) {
				return world;
			}
		}
		return WorldUtil.getWorlds().iterator().next();
	}
}
