package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.bergerkiller.bukkit.config.FileConfiguration;

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
	public static boolean useWorldChatPermissions;
	public static boolean allowPortalNameOverride;
	public static boolean useWorldOperators;
	public static boolean onlyObsidianPortals = false;
	public static boolean isSpoutEnabled = false;
	
	public static MyWorlds plugin;
	private static Logger logger = Logger.getLogger("Minecraft");
	public static void log(Level level, String message) {
		logger.log(level, "[MyWorlds] " + message);
	}
	
	private final MWListener listener = new MWListener();
	
	public String root() {
		return getDataFolder() + File.separator;
	}
	
	public void onEnable() {
		plugin = this;

		//Event registering
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this.listener, this);
        isSpoutEnabled = pm.isPluginEnabled("Spout");
        
        FileConfiguration config = new FileConfiguration(this);
        config.load();
        usePermissions = config.get("usePermissions", false);
        teleportInterval = config.get("teleportInterval", 2000);
        useWaterTeleport = config.get("useWaterTeleport", true);
        timeLockInterval = config.get("timeLockInterval", 20);
        useWorldEnterPermissions = config.get("useWorldEnterPermissions", false);
        usePortalEnterPermissions = config.get("usePortalEnterPermissions", false);
        useWorldTeleportPermissions = config.get("useWorldTeleportPermissions", false);
        usePortalTeleportPermissions = config.get("usePortalTeleportPermissions", false);
        useWorldBuildPermissions = config.get("useWorldBuildPermissions", false);
        useWorldUsePermissions = config.get("useWorldUsePermissions", false);
        useWorldChatPermissions = config.get("useWorldChatPermissions", false);
        allowPortalNameOverride = config.get("allowPortalNameOverride", false);
        useWorldOperators = config.get("useWorldOperators", false);
        onlyObsidianPortals = config.get("onlyObsidianPortals", false);
        String locale = config.get("locale", "default");
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
        
        //init chunk loader
        LoadChunksTask.init();
        
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
