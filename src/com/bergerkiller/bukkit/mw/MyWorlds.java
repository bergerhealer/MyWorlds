package com.bergerkiller.bukkit.mw;

import java.io.File;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import com.bergerkiller.bukkit.common.config.FileConfiguration;
import com.bergerkiller.bukkit.common.PluginBase;

public class MyWorlds extends PluginBase {

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
	public static boolean useAllTeleportPermissions = true;
	public static boolean isSpoutEnabled = false;
	public static boolean onlyPlayerTeleportation = true;
		
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
        
        config.setHeader("This is the configuration of MyWorlds");
        config.addHeader("For more information, you can visit the following websites:");
        config.addHeader("http://dev.bukkit.org/server-mods/my-worlds/");
        config.addHeader("http://forums.bukkit.org/threads/myworlds.31718");
        config.addHeader("http://wiki.bukkit.org/MyWorlds-Plugin");
        
        config.setHeader("usePermissions", "\nWhether the Permissions 3.* plugin should be used for permissions");
        usePermissions = config.get("usePermissions", false);
        
        config.setHeader("teleportInterval", "\nThe interval in miliseconds a player has to wait before being teleported again");
        teleportInterval = config.get("teleportInterval", 2000);
        
        config.setHeader("useWaterTeleport", "\nWhether water stream portals are allowed");
        useWaterTeleport = config.get("useWaterTeleport", true);
        
        config.setHeader("timeLockInterval", "\nThe tick interval at which time is kept locked");
        timeLockInterval = config.get("timeLockInterval", 20);
        
        useWorldEnterPermissions = config.get("useWorldEnterPermissions", false);
        usePortalEnterPermissions = config.get("usePortalEnterPermissions", false);
        useWorldTeleportPermissions = config.get("useWorldTeleportPermissions", false);
        usePortalTeleportPermissions = config.get("usePortalTeleportPermissions", false);
        useWorldBuildPermissions = config.get("useWorldBuildPermissions", false);
        useWorldUsePermissions = config.get("useWorldUsePermissions", false);
        useWorldChatPermissions = config.get("useWorldChatPermissions", false);
        
        config.setHeader("onlyPlayerTeleportation", "\nWhether only players are allowed to teleport through portals");
        onlyPlayerTeleportation = config.get("onlyPlayerTeleportation", true);
        
        config.setHeader("allowPortalNameOverride", "\nWhether portals can be replaced by other portals with the same name on the same world");
        allowPortalNameOverride = config.get("allowPortalNameOverride", true);
        
        config.setHeader("useAllTeleportPermissions", "\nWhether world and portal enter and teleport permissions are also applied to other teleport plugins");
        useAllTeleportPermissions = config.get("useAllTeleportPermissions", true);
        
        config.setHeader("useWorldOperators", "\nWhether each world has it's own operator list");
        useWorldOperators = config.get("useWorldOperators", false);
        
        config.setHeader("onlyObsidianPortals", "\nWhether only portal blocks surrounded by obsidian can teleport players");
        onlyObsidianPortals = config.get("onlyObsidianPortals", false);
        
        config.setHeader("locale", "\nThe locale text file name to use (excluding .txt extension)");
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
		
		//World inventories
		WorldInventory.init(root() + "inventories.yml");
		
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
		
		//World inventories
		WorldInventory.deinit(root() + "inventories.yml");
		
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
