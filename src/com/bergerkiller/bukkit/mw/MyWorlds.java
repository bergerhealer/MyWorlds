package com.bergerkiller.bukkit.mw;

import java.io.File;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import com.bergerkiller.bukkit.common.config.FileConfiguration;
import com.bergerkiller.bukkit.common.PluginBase;

public class MyWorlds extends PluginBase {
	
	public MyWorlds() {
		super(1595, 2000);
	}

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
		
	public String root() {
		return getDataFolder() + File.separator;
	}
	
	@Override
	public void updateDependency(Plugin plugin, String pluginName, boolean enabled) {
		if (pluginName.equals("Spout")) {
			isSpoutEnabled = enabled;
		}
	}
	
	public void enable() {
		plugin = this;

		//Event registering
		this.register(MWListener.class);
		this.register("tpp", "world");  
		
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
		
        //init chunk loader
        LoadChunksTask.init();
        
        //Chunk cache
        WorldManager.init();
	}
	public void disable() {
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
	}
	
	public boolean command(CommandSender sender, String cmdLabel, String[] args) {
		com.bergerkiller.bukkit.mw.commands.Command.execute(sender, cmdLabel, args);
		return true;
	}

	@Override
	public void permissions() {
		this.loadPermissions(Permission.class);
	}

}
