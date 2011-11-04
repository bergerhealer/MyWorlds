package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MyWorlds extends JavaPlugin {
	public static boolean usePermissions;
	public static int teleportInterval;
	public static boolean useWaterTeleport;
	public static int timeLockInterval;
	public static boolean useWorldEnterPermissions;
	public static boolean usePortalEnterPermissions;
	public static boolean useWorldTeleportPermissions;
	public static boolean usePortalTeleportPermissions;
	public static boolean useWorldBuildPermissions;
	public static boolean useWorldUsePermissions;
	public static boolean allowPortalNameOverride;
	public static boolean useWorldOperators;
	
	public static MyWorlds plugin;
	private static Logger logger = Logger.getLogger("Minecraft");
	public static void log(Level level, String message) {
		logger.log(level, "[MyWorlds] " + message);
	}
	
	private final MWEntityListener entityListener = new MWEntityListener();
	private final MWBlockListener blockListener = new MWBlockListener();
	private final MWWorldListener worldListener = new MWWorldListener();
	private final MWPlayerListener playerListener = new MWPlayerListener();
	private final MWWeatherListener weatherListener = new MWWeatherListener();
	
	public String root() {
		return getDataFolder() + File.separator;
	}
	
	public void onEnable() {
		plugin = this;

		//Event registering
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.ENTITY_PORTAL_ENTER, entityListener, Priority.Monitor, this);
        pm.registerEvent(Event.Type.CREATURE_SPAWN, entityListener, Priority.Lowest, this);
        pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Highest, this);
        pm.registerEvent(Event.Type.BLOCK_PLACE, blockListener, Priority.Highest, this);
        pm.registerEvent(Event.Type.BLOCK_PHYSICS, blockListener, Priority.Highest, this); 
        pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Monitor, this);
        pm.registerEvent(Event.Type.PLAYER_TELEPORT, playerListener, Priority.Monitor, this);
        pm.registerEvent(Event.Type.PLAYER_RESPAWN, playerListener, Priority.Highest, this);
        pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Monitor, this);
        pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Highest, this);
        pm.registerEvent(Event.Type.SIGN_CHANGE, blockListener, Priority.Highest, this);  
        pm.registerEvent(Event.Type.WORLD_LOAD, worldListener, Priority.Monitor, this);  
        pm.registerEvent(Event.Type.WORLD_UNLOAD, worldListener, Priority.Monitor, this);  
        pm.registerEvent(Event.Type.WORLD_INIT, worldListener, Priority.Highest, this);
        pm.registerEvent(Event.Type.WEATHER_CHANGE, weatherListener, Priority.Highest, this); 
        

        
        Configuration config = new Configuration(this);
        config.load();
        usePermissions = config.parse("usePermissions", false);
        teleportInterval = config.parse("teleportInterval", 2000);
        useWaterTeleport = config.parse("useWaterTeleport", true);
        timeLockInterval = config.parse("timeLockInterval", 20);
        useWorldEnterPermissions = config.parse("useWorldEnterPermissions", false);
        usePortalEnterPermissions = config.parse("usePortalEnterPermissions", false);
        useWorldTeleportPermissions = config.parse("useWorldTeleportPermissions", false);
        usePortalTeleportPermissions = config.parse("usePortalTeleportPermissions", false);
        useWorldBuildPermissions = config.parse("useWorldBuildPermissions", false);
        useWorldUsePermissions = config.parse("useWorldUsePermissions", false);
        allowPortalNameOverride = config.parse("allowPortalNameOverride", false);
        useWorldOperators = config.parse("useWorldOperators", false);
        String locale = config.parse("locale", "default");
        config.save();
        
        //Localization
        Localization.init(this, locale);
        
        //Permissions
		Permission.init(this);
		
		//Portals
		Portal.init(root() + "portals.txt");

		//World info
		WorldConfig.init(root() + "worlds.yml");
		
        //Commands
        getCommand("tpp").setExecutor(this);
        getCommand("world").setExecutor(this);  
        
        //Chunk cache
        WorldManager.init();
        
        //final msg
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println("[MyWorlds] version " + pdfFile.getVersion() + " is enabled!" );
	}
	public void onDisable() {
		//Portals
		Portal.deinit(root() + "portals.txt");
		
		//World info
		WorldConfig.deinit(root() + "worlds.yml");
		
        WorldManager.deinit();
        Localization.deinit();
        Permission.deinit();
        
		//Abort chunk loader
		LoadChunksTask.deinit();
		
		plugin = null;
		
		System.out.println("My Worlds disabled!");
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String cmdLabel, String[] args) {
		com.bergerkiller.bukkit.mw.commands.Command.execute(sender, cmdLabel, args);
		return true;
	}

}
