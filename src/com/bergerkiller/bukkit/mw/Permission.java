package com.bergerkiller.bukkit.mw;

import java.util.logging.Level;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.bergerkiller.bukkit.common.permissions.IPermissionDefault;
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
    COMMAND_OPPING("world.opping", PermissionDefault.OP, "Sets if the player can add or remove operators to/from a world"),
    COMMAND_TOGGLESPAWNLOADED("world.togglespawnloaded", PermissionDefault.OP, "Sets if the player can toggle spawn chunk loading on or off"),
    COMMAND_SPAWNING("world.spawning", PermissionDefault.OP, "Sets if the player can allow and deny mobs spawning"),
    COMMAND_WEATHER("world.weather", PermissionDefault.OP, "Sets if the player can change the weather on worlds"),
    COMMAND_TIME("world.time", PermissionDefault.OP, "Sets if the player can change the time on worlds"),
    COMMAND_GAMEMODE("world.gamemode", PermissionDefault.OP, "Sets if the player can change the gamemode of a world"),
    COMMAND_SETSPAWN("world.setspawn", PermissionDefault.OP, "Sets if the player can change the spawn point of a world"),
    COMMAND_INVENTORY("world.inventory", PermissionDefault.OP, "Sets if the player can alter the inventory states of a world"),
    COMMAND_TOGGLERESPAWN("world.togglerespawn", PermissionDefault.OP, "Sets if the player can toggle the forced respawn to the world spawn"),
    COMMAND_SPOUTWEATHER("world.spoutweather", PermissionDefault.OP, "Sets if player can toggle virtual weather changes using Spout Plugin"),
    COMMAND_FORMING("world.forming", PermissionDefault.OP, "Sets if the player can toggle snow and ice forming on or off"),
    COMMAND_RELOADWE("world.reloadwe", PermissionDefault.OP, "Sets if players can toggle if worlds reload when empty"),
    GENERAL_TELEPORT("world.teleport", PermissionDefault.OP, "Sets the worlds a player can teleport to using /tpp and /world spawn", 1),
    GENERAL_ENTER("world.enter", PermissionDefault.OP, "Sets if the player can enter a certain world through portals", 1),
    GENERAL_BUILD("world.build", PermissionDefault.OP, "Sets if the player can build in a certain world", 1),
    GENERAL_CHAT("world.chat", PermissionDefault.TRUE, "Sets if the player can chat while being in a certain world", 1),
    GENERAL_CHATALLWORLDS("world.chat", PermissionDefault.OP, "Sets if the player can chat from every world to every world", 2),
    GENERAL_IGNOREGM("world.ignoregamemode", PermissionDefault.FALSE, "Sets if the player game mode is not changed by the world game mode"),
    GENERAL_USE("world.use", PermissionDefault.OP, "Sets if the player can interact with blocks in a certain world", 1),
    GENERAL_KEEPINV("world.keepinventory", PermissionDefault.FALSE, "Sets if the player keeps his inventory while switching worlds"),
    PORTAL_CREATE("portal.create", PermissionDefault.OP, "Sets if the player can create teleport signs"),
    PORTAL_OVERRIDE("portal.override", PermissionDefault.OP, "Sets if the player can replace existing portals"),
    PORTAL_USE("portal.use", PermissionDefault.TRUE, "Sets if the player can use portals", 1),
    PORTAL_TELEPORT("portal.teleport", PermissionDefault.OP, "Sets the portals a player can teleport to using /tpp", 1),
    PORTAL_ENTER("portal.enter", PermissionDefault.OP, "Sets if the player can enter a certain portal", 1),
    COMMAND_TPP("tpp", PermissionDefault.OP, "Sets if the player can teleport to worlds or portals");

	private final String node;
	private final String name;
	private final PermissionDefault def;
	private final String desc;

	private Permission(final String name, final PermissionDefault def, final String desc) {
		this(name, def, desc, 0);
	}

	private Permission(final String name, final PermissionDefault def, final String desc, final int argCount) {
		this.node = "myworlds." + name;
		this.def = def;
		this.desc = desc;
		StringBuilder builder = new StringBuilder(this.node);
		for (int i = 0; i < argCount; i++) {
			builder.append(".*");
		}
		this.name = builder.toString();
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public PermissionDefault getDefault() {
		return this.def;
	}

	@Override
	public String getDescription() {
		return this.desc;
	}

	public boolean hasGlobal(Player player, String name) {
		return has(player, name) || has(player, "*");
	}

	public boolean hasGlobal(Player player, String name1, String name2) {
		return has(player, name1, name2) || has(player, name1, "*") || has(player, "*", name2) || has(player, "*", "*");
	}

	public boolean has(Player player) {
		return has(player, new String[0]);
	}

	public boolean has(Player player, String... args) {
		String node = this.node;
		if (args.length > 0) {
			StringBuilder builder = new StringBuilder(node);
			for (String arg : args) {
				builder.append('.').append(arg);
			}
			node = builder.toString();
		}
		if (permissionHandler != null) {
			//Permissions 3.*
			return permissionHandler.has(player, node);
		} else {
			//Build-in permissions
			return player.hasPermission(node);
		}
	}

	@Override
	public String toString() {
		return this.getName();
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

	public static boolean canEnter(Player player, Portal portal) {
		return canEnterPortal(player, portal.getName());
	}
	public static boolean canEnter(Player player, World world) {
		return canEnterWorld(player, world.getName());
	}
	public static boolean canEnterPortal(Player player, String portalname) {
		if (!Permission.PORTAL_USE.hasGlobal(player, portalname)) {
			return false;
		}
		if (!MyWorlds.usePortalEnterPermissions) {
			return true;
		}
		return Permission.PORTAL_ENTER.hasGlobal(player, portalname);
	}
	public static boolean canEnterWorld(Player player, String worldname) {
		if (!MyWorlds.useWorldEnterPermissions) {
			return true;
		}
		if (player.getWorld().getName().equalsIgnoreCase(worldname)) {
			return true;
		}
		return Permission.GENERAL_ENTER.hasGlobal(player, worldname);
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
		return Permission.GENERAL_BUILD.hasGlobal(player, worldname);
	}
	public static boolean canUse(Player player, String worldname) {
		if (player == null) return true;
		if (!MyWorlds.useWorldUsePermissions) return true;
		return Permission.GENERAL_USE.hasGlobal(player, worldname);
	}
	
	public static boolean canChat(Player player) {
		return canChat(player, player);
	}
	public static boolean canChat(Player player, Player with) {
		if (player == null) return true;
		if (!MyWorlds.useWorldChatPermissions) return true;
		final String from = player.getWorld().getName().toLowerCase();
		final String to = with.getWorld().getName().toLowerCase();
		if (Permission.GENERAL_CHAT.hasGlobal(player, from, to)) {
			return true;
		}
		if (from.equals(to)) {
			return Permission.GENERAL_CHAT.hasGlobal(player, from);
		} else {
			return false;
		}
	}
}
