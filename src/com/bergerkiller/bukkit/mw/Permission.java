package com.bergerkiller.bukkit.mw;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;
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
	public static void deinit() {
		permissionHandler = null;
	}
	
	public static boolean has(Player player, String command) {
		if (permissionHandler != null) {
			//Permissions 3.*
			return permissionHandler.has(player, "myworlds." + command);
		} else {
			//Build-in permissions
			return player.hasPermission("myworlds." + command);
		}
	}
	public static boolean hasGlobal(Player player, String node, String name) {
		return has(player, node + name) || has(player, node + "*");
	}
		
	public static boolean handleTeleport(Entity entity, Portal portal) {
		return handleTeleport(entity, portal.getName(), portal.getDestinationName(), portal.getDestinationDisplayName(), portal.getDestination());
	}
	public static boolean handleTeleport(Entity entity, String toportalname, Location portalloc) {
		return handleTeleport(entity, null, toportalname, toportalname, portalloc);
	}
	public static boolean handleTeleport(Entity entity, String fromportalname, String toportalname, String toportaldispname, Location portalloc) {
		if (toportaldispname == null || portalloc == null) return false;
		if (entity instanceof Player) {
			Player p = (Player) entity;
			if (fromportalname != null && !canEnterPortal(p, fromportalname)) {
				Localization.message(p, "portal.noaccess");
				return false;
			} else {
				p.sendMessage(Localization.getPortalEnter(toportalname, toportaldispname));
			}
		}
		return handleTeleport(entity, portalloc, false);
	}
	
	public static boolean handleTeleport(Entity entity, Location to) {
		return handleTeleport(entity, to, true);
	}
	public static boolean handleTeleport(Entity entity, Location to, boolean showworld) {
		if (to == null) return false;
		if (entity instanceof Player) {
			Player p = (Player) entity;
			if (!canEnter(p, to.getWorld())) {
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
		if (!has(player, "portal.use")) return false;
		if (!MyWorlds.usePortalEnterPermissions) return true;	
		return hasGlobal(player, "portal.enter.", portalname);
	}
	public static boolean canEnterWorld(Player player, String worldname) {
		if (!MyWorlds.useWorldEnterPermissions) return true;
		if (player.getWorld().getName().equalsIgnoreCase(worldname)) return true;
		return hasGlobal(player, "world.enter.", worldname);
	}
	
	public static boolean canTeleportPortal(Player player, String portalname) {
		if (!MyWorlds.usePortalTeleportPermissions) return true;
		return hasGlobal(player, "portal.teleport.", portalname);
	}
	public static boolean canTeleportWorld(Player player, String worldname) {
		if (!MyWorlds.useWorldTeleportPermissions) return true;
		if (player.getWorld().getName().equalsIgnoreCase(worldname)) return true;
		return hasGlobal(player, "world.teleport.", worldname);
	}

	public static boolean canBuild(Player player) {
		if (player == null) return true;
		return canBuild(player, player.getWorld().getName());
	}
	public static boolean canUse(Player player) {
		if (player == null) return true;
		return canUse(player, player.getWorld().getName());
	}
	public static boolean canBuild(Player player, String worldname) {
		if (player == null) return true;
		if (!MyWorlds.useWorldBuildPermissions) return true;
		return hasGlobal(player, "world.build.", worldname);
	}
	public static boolean canUse(Player player, String worldname) {
		if (player == null) return true;
		if (!MyWorlds.useWorldUsePermissions) return true;
		return hasGlobal(player, "world.use.", worldname);
	}
	
	public static boolean canChat(Player player) {
		return canChat(player, player);
	}
	public static boolean canChat(Player player, Player with) {
		if (player == null) return true;
		if (!MyWorlds.useWorldChatPermissions) return true;
		final String from = player.getWorld().getName().toLowerCase();
		final String to = with.getWorld().getName().toLowerCase();
		if (has(player, "world.chat.*.*")) return true;
		if (has(player, "world.chat." + from + "." + to)) return true;
		if (has(player, "world.chat." + from + ".*")) return true;
		if (has(player, "world.chat.*." + to)) return true;
		if (from.equals(to)) {
			if (has(player, "world.chat.*")) return true;
			return has(player, "world.chat." + from);
		} else {
			return false;
		}
	}
}
