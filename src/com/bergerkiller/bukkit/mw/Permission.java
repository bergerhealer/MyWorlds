package com.bergerkiller.bukkit.mw;

import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijiko.permissions.PermissionHandler;

public class Permission {
	private static PermissionHandler permissionHandler = null; //Permissions 3.* ONLY
	public static void init(JavaPlugin plugin) {
		if (MyWorlds.usePermissions) {
			Plugin permissionsPlugin = plugin.getServer().getPluginManager().getPlugin("Permissions");
			if (permissionsPlugin == null) {
				MyWorlds.log(Level.WARNING, "Permission system not detected, defaulting to build-in permissions!");
			} else {
				permissionHandler = ((Permissions) permissionsPlugin).getHandler();
				MyWorlds.log(Level.INFO, "Found and will use permissions plugin "+((Permissions)permissionsPlugin).getDescription().getFullName());
			}
		} else {
			MyWorlds.log(Level.INFO, "Using build-in 'Bukkit SuperPerms' as permissions plugin!");;
		}
	}
	public static boolean has(CommandSender sender, String command) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (permissionHandler != null) {
				//Permissions 3.*
				return permissionHandler.has(player, "myworlds." + command);
			} else {
				//Build-in permissions
				return ((Player) sender).hasPermission("myworlds." + command);
			}
		} else {
			if (command.equalsIgnoreCase("world.spawn")) return false;
			if (command.equalsIgnoreCase("tpp")) return false;
			return true;
		}
	}
	
}
