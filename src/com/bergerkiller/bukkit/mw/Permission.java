package com.bergerkiller.bukkit.mw;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
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
	
	public static boolean handleTeleport(Entity entity, String portalname, Location portalloc) {
		if (portalname == null || portalloc == null) return false;
		if (entity instanceof Player) {
			Player p = (Player) entity;
			if (has(p, "portal.use")) {
				if (MyWorlds.usePortalEnterPermissions && !has(p, "portal.enter." + portalname)) {
					Localization.message(p, "portal.noaccess");
					return false;
				} else {
					p.sendMessage(Localization.getPortalEnter(portalname));
				}
			} else {
				Localization.message(p, "portal.noaccess");
				return false;
			}
		}
		return handleTeleport(entity, portalloc, false);
	}
	public static boolean handleTeleport(Entity entity, Portal portal) {
		return handleTeleport(entity, portal.getDestinationName(), portal.getDestination());
	}
	public static boolean handleTeleport(Entity entity, Location to) {
		return handleTeleport(entity, to, true);
	}
	public static boolean handleTeleport(Entity entity, Location to, boolean showworld) {
		if (to == null) return false;
		if (entity instanceof Player) {
			Player p = (Player) entity;
			if (MyWorlds.useWorldEnterPermissions && !has(p, "world.enter." + to.getWorld().getName())) {
				Localization.message(p, "world.noaccess");
				return false;
			} else if (showworld) {
				p.sendMessage(Localization.getWorldEnter(to.getWorld()));
			}
		}
		entity.teleport(to);
		return true;
	}
		
	public static boolean canEnter(Player player, Portal portal) {
		return canEnterPortal(player, portal.getName());
	}
	public static boolean canEnter(Player player, World world) {
		return canEnterWorld(player, world.getName());
	}
	public static boolean canEnterPortal(Player player, String portalname) {
		if (!MyWorlds.usePortalEnterPermissions) return true;	
		return has(player, "portal.enter." + portalname);
	}
	public static boolean canEnterWorld(Player player, String worldname) {
		if (!MyWorlds.useWorldEnterPermissions) return true;
		return has(player, "world.enter." + worldname);
	}
	
}
