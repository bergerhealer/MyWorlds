package com.bergerkiller.bukkit.mw;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;

import com.bergerkiller.bukkit.common.permissions.PermissionEnum;

public class Permission extends PermissionEnum {
    public static final Permission COMMAND_INVENTORY_CLEAR = new Permission("world.clearinventory", PermissionDefault.OP, "Allows clearing a player's inventory in a specific world");
    public static final Permission COMMAND_LIST = new Permission("world.list", PermissionDefault.OP, "Sets if the player can list all worlds on the server");
    public static final Permission COMMAND_INFO = new Permission("world.info", PermissionDefault.OP, "Sets if the player can see world information, such as the seed and size");
    public static final Permission COMMAND_CONFIG = new Permission("world.config", PermissionDefault.OP, "Sets if the player can manually load, save and reset the world configuration");
    public static final Permission COMMAND_PORTALS = new Permission("world.portals", PermissionDefault.OP, "Sets if the player can list all portals on the server");
    public static final Permission COMMAND_GIVE_PORTAL = new Permission("world.giveportal", PermissionDefault.OP, "Sets if the player can give himself portal blocks for building portal structures");
    public static final Permission COMMAND_LISTGEN = new Permission("world.listgenerators", PermissionDefault.OP, "Sets if the player can list all chunk generators on the server");
    public static final Permission COMMAND_SETPORTAL = new Permission("world.setportal", PermissionDefault.OP, "Sets if the player can change the default portal destination on the world");
    public static final Permission COMMAND_LOAD = new Permission("world.load", PermissionDefault.OP, "Sets if the player can load unloaded worlds (not create)");
    public static final Permission COMMAND_LOADSPECIAL = new Permission("world.loadspecial", PermissionDefault.OP, "Sets if a player can load a world with a new chunk generator (can corrupt worlds!)");
    public static final Permission COMMAND_UNLOAD = new Permission("world.unload", PermissionDefault.OP, "Sets if the player can unload loaded worlds (not create)");
    public static final Permission COMMAND_CREATE = new Permission("world.create", PermissionDefault.OP, "Sets if the player can create worlds (not replace)");
    public static final Permission COMMAND_SPAWN = new Permission("world.spawn", PermissionDefault.OP, "Sets if the player can teleport to world spawn points");
    public static final Permission COMMAND_SPAWN_OTHERS = new Permission("world.spawnothers", PermissionDefault.OP, "Sets whether the player can teleport other players to the spawn of on any world. Overrides the normal permission.");
    public static final Permission COMMAND_REJOIN = new Permission("world.rejoin", PermissionDefault.OP, "Sets if the player can teleport to rejoin the last world they were on, permission per world", 1);
    public static final Permission COMMAND_REJOIN_OTHERS = new Permission("world.rejoinothers", PermissionDefault.OP, "Sets whether the player can force a rejoin of other players on any world. Overrides the normal permission.");
    public static final Permission COMMAND_EVACUATE = new Permission("world.evacuate", PermissionDefault.OP, "Sets if the player can clear a world from its players");
    public static final Permission COMMAND_REPAIR = new Permission("world.repair", PermissionDefault.OP, "Sets if the player can repair damaged worlds (only if broken)");
    public static final Permission COMMAND_SAVE = new Permission("world.save", PermissionDefault.OP, "Sets if the player can save worlds");
    public static final Permission COMMAND_SETSAVING = new Permission("world.setsaving", PermissionDefault.OP, "Sets if the player can toggle world auto-saving on or off");
    public static final Permission COMMAND_DELETE = new Permission("world.delete", PermissionDefault.OP, "Sets if the player can permanently delete worlds");
    public static final Permission COMMAND_COPY = new Permission("world.copy", PermissionDefault.OP, "Sets if the player can clone worlds");
    public static final Permission COMMAND_DIFFICULTY = new Permission("world.difficulty", PermissionDefault.OP, "Sets if the player can change the difficulty setting of worlds");
    public static final Permission COMMAND_TOGGLEPVP = new Permission("world.togglepvp", PermissionDefault.OP, "Sets if the player can change the PvP setting of worlds");
    public static final Permission COMMAND_OPPING = new Permission("world.opping", PermissionDefault.OP, "Sets if the player can add or remove operators to/from a world");
    public static final Permission COMMAND_TOGGLESPAWNLOADED = new Permission("world.togglespawnloaded", PermissionDefault.OP, "Sets if the player can toggle spawn chunk loading on or off");
    public static final Permission COMMAND_SPAWNING = new Permission("world.spawning", PermissionDefault.OP, "Sets if the player can allow and deny mobs spawning");
    public static final Permission COMMAND_WEATHER = new Permission("world.weather", PermissionDefault.OP, "Sets if the player can change the weather on worlds");
    public static final Permission COMMAND_TIME = new Permission("world.time", PermissionDefault.OP, "Sets if the player can change the time on worlds");
    public static final Permission COMMAND_GAMEMODE = new Permission("world.gamemode", PermissionDefault.OP, "Sets if the player can change the gamemode of a world");
    public static final Permission COMMAND_SETSPAWN = new Permission("world.setspawn", PermissionDefault.OP, "Sets if the player can change the (re)spawn point of a world, and whether bed respawning is enabled");
    public static final Permission COMMAND_SETREMEMBERLASTPOS = new Permission("world.setrememberlastpos", PermissionDefault.OP, "Sets if the player can change whether worlds remember the last player positions");
    public static final Permission COMMAND_INVENTORY = new Permission("world.inventory", PermissionDefault.OP, "Sets if the player can alter the inventory states of a world");
    public static final Permission COMMAND_FORMING = new Permission("world.forming", PermissionDefault.OP, "Sets if the player can toggle snow and ice forming on or off");
    public static final Permission COMMAND_RELOADWE = new Permission("world.reloadwe", PermissionDefault.OP, "Sets if players can toggle if worlds reload when empty");
    public static final Permission COMMAND_HUNGER = new Permission("world.hunger", PermissionDefault.OP, "Sets if players can toggle hunger on or off for worlds");
    public static final Permission COMMAND_ENABLECREDITS = new Permission("world.enablecredits", PermissionDefault.TRUE, "Enables a player to view the end-game credits a second time, after already completing the end before");
    public static final Permission COMMAND_ADVANCEMENTS = new Permission("world.advancements", PermissionDefault.OP, "Sets if players can toggle advancements on/off for a world");
    public static final Permission COMMAND_ALIAS = new Permission("world.alias", PermissionDefault.OP, "Sets if the player can change the (PlaceholderAPI) world name alias");
    public static final Permission COMMAND_LAST_POSITION = new Permission("world.lastposition", PermissionDefault.OP, "Shows the last player positions on other worlds and allows teleporting to them");
    public static final Permission COMMAND_PLAYERLIMIT = new Permission("world.playerlimit", PermissionDefault.OP, "Sets if players can modify the player count limits set for a world");
    public static final Permission COMMAND_SETPORTALOPTION = new Permission("world.setportaloption", PermissionDefault.OP, "Sets if players can modify portal sign options, such as whether mobs can use it");
    public static final Permission COMMAND_AUTOLOAD = new Permission("world.autoload", PermissionDefault.OP, "Sets if the player can set whether a world is loaded on startup or not");
    public static final Permission COMMAND_TPP = new Permission("tpp", PermissionDefault.OP, "Sets if the player can teleport to any worlds or portals, ignoring the per-portal/world permissions");
    public static final Permission GENERAL_TELEPORT = new Permission("world.teleport", PermissionDefault.OP, "Sets the worlds a player can teleport to using /tpp and /world spawn", 1);
    public static final Permission GENERAL_ENTER = new Permission("world.enter", PermissionDefault.OP, "Sets if the player can enter a certain world through portals", 1);
    public static final Permission GENERAL_BUILD = new Permission("world.build", PermissionDefault.OP, "Sets if the player can build in a certain world", 1);
    public static final Permission GENERAL_CHAT = new Permission("world.chat", PermissionDefault.TRUE, "Sets if the player can chat while being in a certain world", 1);
    public static final Permission GENERAL_CHATALLWORLDS = new Permission("world.chat", PermissionDefault.OP, "Sets if the player can chat from every world to every world", 2);
    public static final Permission GENERAL_IGNOREGM = new Permission("world.ignoregamemode", PermissionDefault.FALSE, "Sets if the player game mode is not changed by the world game mode");
    public static final Permission GENERAL_USE = new Permission("world.use", PermissionDefault.OP, "Sets if the player can interact with blocks in a certain world", 1);
    public static final Permission GENERAL_KEEPINV = new Permission("world.keepinventory", PermissionDefault.FALSE, "Sets if the player keeps his inventory while switching worlds");
    public static final Permission GENERAL_KEEPLASTPOS = new Permission("world.keeplastpos", PermissionDefault.FALSE, "Sets if the player overrides the world rememberlastpos setting to always permit it");
    public static final Permission GENERAL_LINKNETHER = new Permission("world.linknether", PermissionDefault.TRUE, "Sets if the player can spawn nether portals on other worlds when a nether link is enabled for a world");
    public static final Permission GENERAL_BYPASSPLAYERLIMITS = new Permission("world.bypassplayerlimit", PermissionDefault.FALSE, "Sets if the player bypasses the per-world player limit (if set)");
    public static final Permission PORTAL_CREATE = new Permission("portal.create", PermissionDefault.OP, "Sets if the player can create teleport signs");
    public static final Permission PORTAL_OVERRIDE = new Permission("portal.override", PermissionDefault.OP, "Sets if the player can replace existing portals");
    public static final Permission PORTAL_USE = new Permission("portal.use", PermissionDefault.TRUE, "Sets if the player can use portals", 1);
    public static final Permission PORTAL_TELEPORT = new Permission("portal.teleport", PermissionDefault.OP, "Sets the portals a player can teleport to using /tpp", 1);
    public static final Permission SUGGESTIONS = new Permission("command.suggestions", PermissionDefault.TRUE, "Sets whether the player can see myworlds command suggestions");

    private Permission(final String name, final PermissionDefault def, final String desc) {
        super("myworlds." + name, def, desc, 0);
    }

    private Permission(final String name, final PermissionDefault def, final String desc, final int argCount) {
        super("myworlds." + name, def, desc, argCount);
    }

    public static boolean canEnter(Player player, Portal portal) {
        return canEnterPortal(player, portal.getName());
    }
    public static boolean canEnter(Player player, World world) {
        return canEnterWorld(player, world.getName());
    }
    public static boolean canEnterPortal(Player player, String portalname) {
        return Permission.PORTAL_USE.has(player, portalname);
    }
    public static boolean canEnterWorld(Player player, String worldname) {
        if (!MyWorlds.useWorldEnterPermissions) {
            return true;
        }
        if (player.getWorld().getName().equalsIgnoreCase(worldname)) {
            return true;
        }
        return Permission.GENERAL_ENTER.has(player, worldname);
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
        return Permission.GENERAL_BUILD.has(player, worldname);
    }
    public static boolean canUse(Player player, String worldname) {
        if (player == null) return true;
        if (!MyWorlds.useWorldUsePermissions) return true;
        return Permission.GENERAL_USE.has(player, worldname);
    }
    
    public static boolean canChat(Player player) {
        return canChat(player, player);
    }
    public static boolean canChat(Player player, Player with) {
        if (player == null) return true;
        if (!MyWorlds.useWorldChatPermissions) return true;
        final String from = player.getWorld().getName().toLowerCase();
        final String to = with.getWorld().getName().toLowerCase();
        if (Permission.GENERAL_CHAT.has(player, from, to)) {
            return true;
        }
        if (from.equals(to)) {
            return Permission.GENERAL_CHAT.has(player, from);
        } else {
            return false;
        }
    }
}
