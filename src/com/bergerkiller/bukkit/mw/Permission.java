package com.bergerkiller.bukkit.mw;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.bergerkiller.bukkit.common.permissions.IPermissionDefault;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijiko.permissions.PermissionHandler;

public enum Permission implements IPermissionDefault {
	COMMAND_LIST("world.list", PermissionDefault.OP, "Sets if the player can list all worlds on the server"),
    COMMAND_INFO("world.info", PermissionDefault.OP, "Sets if the player can see world information, such as the seed and size"),
    COMMAND_CONFIG("world.config", PermissionDefault.OP, "Sets if the player can manually load and save the world configuration"),
    COMMAND_PORTALS("world.portals", PermissionDefault.OP, "Sets if the player can list all portals on the server"),
    COMMAND_LISTGEN("world.listgenerators", PermissionDefault.OP, "Sets if the player can list all chunk generators on the server"),
    COMMAND_SETPORTAL("world.setportal", PermissionDefault.OP, "Sets if the player can change the default portal destination on the world"),
    COMMAND_LOAD("world.load", PermissionDefault.OP, "Sets if the player can load unloaded worlds (not create)"),
    COMMAND_UNLOAD("world.unload", PermissionDefault.OP, "Sets if the player can unload loaded worlds (not create)"),
    COMMAND_CREATE("world.create", PermissionDefault.OP, "Sets if the player can create worlds (not replace)"),
    COMMAND_SPAWN("world.spawn", PermissionDefault.OP, "Sets if the player can teleport to world spawn points"),
    COMMAND_EVACUATE("world.evacuate", PermissionDefault.OP, "Sets if the player can clear a world from its players"),
    COMMAND_REPAIR("world.repair", PermissionDefault.OP, "Sets if the player can repair damaged worlds (only if broken)"),
    COMMAND_SAVE("world.save", PermissionDefault.OP, "Sets if the player can save worlds"),
    COMMAND_SETSAVING("world.setsaving", PermissionDefault.OP, "Sets if the player can toggle world auto-saving on or off"),
    COMMAND_DELETE("world.delete", PermissionDefault.OP, "Sets if the player can permanently delete worlds"),
    COMMAND_COPY("world.copy", PermissionDefault.FALSE, "Sets if the player can clone worlds"),
    COMMAND_DIFFICULTY("world.difficulty", PermissionDefault.OP, "Sets if the player can change the difficulty setting of worlds"),
    COMMAND_TOGGLEPVP("world.togglepvp", PermissionDefault.OP, "Sets if the player can change the PvP setting of worlds"),
    COMMAND_OP("world.op", PermissionDefault.OP, "Sets if the player can add operators to a world"),
    COMMAND_DEOP("world.deop", PermissionDefault.OP, "Sets if the player can remove operator from a world"),
    COMMAND_TOGGLESPAWNLOADED("world.togglespawnloaded", PermissionDefault.OP, "Sets if the player can toggle spawn chunk loading on or off"),
    COMMAND_ALLOWSPAWN("world.allowspawn", PermissionDefault.OP, "Sets if the player can start mobs spawning"),
    COMMAND_DENYSPAWN("world.denyspawn", PermissionDefault.OP, "Sets if the player can stop mobs from spawning"),
    COMMAND_WEATHER("world.weather", PermissionDefault.OP, "Sets if the player can change the weather on worlds"),
    COMMAND_TIME("world.time", PermissionDefault.OP, "Sets if the player can change the time on worlds"),
    COMMAND_GAMEMODE("world.gamemode", PermissionDefault.OP, "Sets if the player can change the gamemode of a world"),
    COMMAND_SETSPAWN("world.setspawn", PermissionDefault.OP, "Sets if the player can change the spawn point of a world"),
    COMMAND_INVENTORY("world.inventory", PermissionDefault.OP, "Sets if the player can alter the inventory states of a world"),
    GENERAL_TELEPORTALL("world.teleport.*", PermissionDefault.OP, "Sets the worlds a player can teleport to using /tpp and /world spawn"),
    GENERAL_ENTERALL("world.enter.*", PermissionDefault.OP, "Sets if the player can enter a certain world through portals"),
    GENERAL_BUILDALL("world.build.*", PermissionDefault.OP, "Sets if the player can build in a certain world"),
    GENERAL_CHATALL("world.chat.*", PermissionDefault.TRUE, "Sets if the player can chat while being in a certain world"),
    GENERAL_CHATALLWORLDS("world.chat.*.*", PermissionDefault.OP, "Sets if the player can chat from every world to every world"),
    GENERAL_IGNOREGM("world.ignoregamemode", PermissionDefault.FALSE, "Sets if the player game mode is not changed by the world game mode"),
    GENERAL_USEALL("world.use.*", PermissionDefault.OP, "Sets if the player can interact with blocks in a certain world"),
    GENERAL_KEEPINV("world.keepinventory", PermissionDefault.OP, "Sets if the player keeps his inventory while switching worlds"),
    PORTAL_CREATE("portal.create", PermissionDefault.OP, "Sets if the player can create teleport signs"),
    PORTAL_OVERRIDE("portal.override", PermissionDefault.OP, "Sets if the player can replace existing portals"),
    PORTAL_USE("portal.use", PermissionDefault.TRUE, "Sets if the player can use portals"),
    PORTAL_TELEPORTALL("portal.teleport.*", PermissionDefault.OP, "Sets the portals a player can teleport to using /tpp"),
    PORTAL_ENTERALL("portal.enter.*", PermissionDefault.OP, "Sets if the player can enter a certain portal"),
    COMMAND_TPP("tpp", PermissionDefault.OP, "Sets if the player can teleport to worlds or portals");

	private final String name;
	private final PermissionDefault def;
	private final String desc;
	private Permission(final String name, final PermissionDefault def, final String desc) {
		this.name = name;
		this.def = def;
		this.desc = desc;
	}
	
	@Override
	public String getName() {
		return "myworlds." + this.name;
	}
	@Override
	public PermissionDefault getDefault() {
		return this.def;
	}
	@Override
	public String getDescription() {
		return this.desc;
	}
	
	public boolean has(Player player) {
		return has(player, this.name);
	}
	
	public String toString() {
		return this.name;
	}
	
	private static PermissionHandler permissionHandler = null; //Permissions 3.* ONLY
	public static void init(JavaPlugin plugin) {
		if (MyWorlds.usePermissions) {
			Plugin permissionsPlugin = plugin.getServer().getPluginManager().getPlugin("Permissions");
			if (permissionsPlugin == null) {
				MyWorlds.plugin.log(Level.WARNING, "Permission system not detected, defaulting to build-in permissions!");
			} else {
				permissionHandler = ((Permissions) permissionsPlugin).getHandler();
				MyWorlds.plugin.log(Level.INFO, "Found and will use permissions plugin "+((Permissions)permissionsPlugin).getDescription().getFullName());
			}
		} else {
			MyWorlds.plugin.log(Level.INFO, "Using build-in 'Bukkit SuperPerms' as permissions plugin!");;
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
		EntityUtil.teleport(MyWorlds.plugin, entity, to);
		if (entity instanceof Player) {
			Portal.notifyNoMove((Player) entity);
		}
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
