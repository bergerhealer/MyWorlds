package com.bergerkiller.bukkit.mw;

import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijiko.permissions.PermissionHandler;

public class Permission {
	private static PermissionHandler permissionHandler = null;
	public static void init(JavaPlugin plugin) {
		Plugin permissionsPlugin = plugin.getServer().getPluginManager().getPlugin("Permissions");
		if (permissionsPlugin == null) {
		    MyWorlds.log(Level.WARNING, "[MyWorlds] Permission system not detected, defaulting to build-in permissions!");
		} else {
			permissionHandler = ((Permissions) permissionsPlugin).getHandler();
			MyWorlds.log(Level.INFO, "[MyWorlds] Found and will use permissions plugin "+((Permissions)permissionsPlugin).getDescription().getFullName());
		}
	}
	public static boolean has(CommandSender sender, String command) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (permissionHandler == null) {
				//some standard stuff that is allowed for everyone
				if (command.equalsIgnoreCase("portal.use")) return true;
				final String[] opCommands = new String[] {"world.load", "world.unload",
						"world.create", "world.list", "world.info",
						"world.spawn", "world.repair", "world.save", "world.evacuate", "world.portals", "tpp", "portal.create"};
				for (String comm : opCommands) {
					if (comm.equalsIgnoreCase(command)) return player.isOp();
				}
				return false;
			} else {
				return permissionHandler.has(player, "myworlds." + command);
			}
		} else {
			if (command.equalsIgnoreCase("world.spawn")) return false;
			if (command.equalsIgnoreCase("tpp")) return false;
			return true;
		}
	}
	
}
