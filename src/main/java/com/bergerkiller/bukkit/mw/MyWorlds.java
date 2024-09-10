package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.bases.IntVector3;
import com.bergerkiller.bukkit.common.softdependency.SoftDependency;
import com.bergerkiller.bukkit.mw.mythicdungeons.MythicDungeonsHelper;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.config.FileConfiguration;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.mw.advancement.AdvancementManager;
import com.bergerkiller.bukkit.mw.commands.registry.MyWorldsCommands;
import com.bergerkiller.bukkit.mw.papi.PlaceholderAPIHandlerWithExpansions;
import com.bergerkiller.bukkit.mw.patch.WorldInventoriesDupingPatch;
import com.bergerkiller.bukkit.mw.playerdata.PlayerDataMigrator;
import com.bergerkiller.bukkit.mw.portal.PlayerRespawnHandler;
import com.bergerkiller.bukkit.mw.portal.PortalEnterEventDebouncer;
import com.bergerkiller.bukkit.mw.portal.PortalFilter;
import com.bergerkiller.bukkit.mw.portal.PortalSignList;
import com.bergerkiller.bukkit.mw.portal.EntityStasisHandler;
import com.bergerkiller.bukkit.mw.portal.NetherPortalSearcher;
import com.bergerkiller.bukkit.mw.portal.PortalTeleportationCooldown;
import com.bergerkiller.bukkit.mw.portal.handlers.BetterPortalsHandler;
import com.bergerkiller.bukkit.common.PluginBase;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.component.LibraryComponent;

public class MyWorlds extends PluginBase {
    private static final String MULTIVERSE_NAME = "Multiverse-Core";
    private static final String PLACEHOLDERAPI_NAME = "PlaceholderAPI";
    private static final String BETTERPORTALS_NAME = "BetterPortals";

    /*
     * ========================================================================
     * =============== Configurations loaded from config.yml ==================
     * ========================================================================
     */
    public static int teleportInterval;
    public static int timeLockInterval;
    public static boolean useWorldEnterPermissions;
    public static boolean useWorldBuildPermissions;
    public static boolean useWorldUsePermissions;
    public static boolean useWorldChatPermissions;
    public static boolean keepLastPositionPermissionEnabled;
    public static boolean allowPortalNameOverride;
    public static boolean useWorldOperators;
    public static boolean onlyObsidianPortals = false;
    public static boolean importFromMultiVerse = true;
    public static boolean useWorldInventories;
    public static boolean storeInventoryInMainWorld;
    public static boolean calculateWorldSize;
    public static double maxPortalSignDistance;
    private static String mainWorld;
    public static boolean forceMainWorldSpawn;
    public static boolean forceJoinOnMainWorld;
    public static boolean forceGamemodeChanges;
    public static boolean overridePortalPhysics;
    public static IntVector3 portalSearchMatchRadius = new IntVector3(32, 4, 32);
    public static boolean alwaysInstantPortal;
    public static boolean ignoreEggSpawns;
    public static boolean ignoreBreedingSpawns;
    public static boolean debugLogGMChanges;
    public static boolean portalToLastPosition;
    public static boolean keepInventoryPermissionEnabled;
    public static boolean portalSignsTeleportMobs;
    public static boolean worldTime24Hours;
    // Portals
    public static boolean waterPortalEnabled;
    public static boolean waterPortalStrict;
    public static boolean endPortalEnabled;
    public static boolean netherPortalEnabled;
    /* ===================================================================== */

    /*
     * ========================================================================
     * ========================== Plugin internals ============================
     * ========================================================================
     */
    public static boolean isSpoutPluginEnabled = false;
    public static boolean isMultiverseEnabled = false;
    // World to disable keepspawnloaded for
    private HashSet<String> spawnDisabledWorlds = new HashSet<String>();
    final MWListener listener = new MWListener(this);
    private MWPlayerChatListener chatListener = null; // Only initialized if used
    private MWPlayerDataController dataController;
    private final MyWorldsCommands commands = new MyWorldsCommands(this);
    private final WorldInventoriesDupingPatch worldDupingPatch = new WorldInventoriesDupingPatch();
    private final NetherPortalSearcher netherPortalSearcher = new NetherPortalSearcher(this);
    private final PortalTeleportationCooldown portalTeleportationCooldown = new PortalTeleportationCooldown(this);
    private final EntityStasisHandler entityStasisHandler = new EntityStasisHandler(this);
    private final PlayerRespawnHandler endRespawnHandler = new PlayerRespawnHandler(this);
    private final AdvancementManager advancementManager = AdvancementManager.create(this);
    private final PortalSignList portalSignList = new PortalSignList(this);
    private final AutoSaveTask autoSaveTask = new AutoSaveTask(this);
    private final PortalEnterEventDebouncer portalEnterEventDebouncer = new PortalEnterEventDebouncer(this, listener::onPortalEnter);
    private final PlayerDataMigrator migrator = new PlayerDataMigrator(this);
    private final List<PortalFilter> portalFilters = new ArrayList<>();
    private LibraryComponent placeholderApi = null;
    public static MyWorlds plugin;

    private final SoftDependency<MythicDungeonsHelper> mythicDungeons = new SoftDependency<MythicDungeonsHelper>(this, "MythicDungeons", MythicDungeonsHelper.DISABLED) {
        //@Override // SoftDependency lib v1.03
        protected boolean identify(Plugin plugin) {
            return plugin.getClass().getName().equals("net.playavalon.mythicdungeons.MythicDungeons");
        }

        @Override
        protected MythicDungeonsHelper initialize(Plugin plugin) throws Error, Exception {
            // For backwards support of older bkcl (softdep lib v1.02 and lower)
            if (!identify(plugin)) {
                return MythicDungeonsHelper.DISABLED;
            }

            return MythicDungeonsHelper.init(MyWorlds.this, plugin);
        }

        @Override
        public void onEnable() {
            // For backwards support of older bkcl (softdep lib v1.02 and lower)
            if (get() == MythicDungeonsHelper.DISABLED) {
                return;
            }

            getLogger().log(Level.INFO, "Mythic Dungeons detected: dungeon instances will automatically share inventory settings");
        }
    };

    public PortalSignList getPortalSignList() {
        return this.portalSignList;
    }

    public AdvancementManager getAdvancementManager() {
        return this.advancementManager;
    }

    public PlayerRespawnHandler getEndRespawnHandler() {
        return this.endRespawnHandler;
    }

    public EntityStasisHandler getEntityStasisHandler() {
        return this.entityStasisHandler;
    }

    public PortalTeleportationCooldown getPortalTeleportationCooldown() {
        return this.portalTeleportationCooldown;
    }

    public NetherPortalSearcher getNetherPortalSearcher() {
        return this.netherPortalSearcher;
    }

    public PortalEnterEventDebouncer getPortalEnterEventDebouncer() {
        return this.portalEnterEventDebouncer;
    }

    public PlayerDataMigrator getPlayerDataMigrator() {
        return this.migrator;
    }

    @Override
    public int getMinimumLibVersion() {
        return 12001; //TODO: Change back to Common.VERSION
    }

    public String root() {
        return getDataFolder() + File.separator;
    }

    public MythicDungeonsHelper getMythicDungeonsHelper() {
        return mythicDungeons.get();
    }

    @Override
    public void updateDependency(Plugin plugin, String pluginName, boolean enabled) {
        if (pluginName.equals("Spout")) {
            isSpoutPluginEnabled = enabled;
        }
        if (pluginName.equals(MULTIVERSE_NAME)) {
            isMultiverseEnabled = enabled;
        }
        if (pluginName.equals(PLACEHOLDERAPI_NAME)) {
            setPAPIIntegrationEnabled(enabled, false);
        }
        if (enabled && pluginName.equals(BETTERPORTALS_NAME)) {
            try {
                this.portalFilters.add(new BetterPortalsHandler(plugin));
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Failed to add BetterPortals support", t);
            }
        }

        // Disable filters if they use this plugin
        if (!enabled) {
            for (Iterator<PortalFilter> iter = this.portalFilters.iterator(); iter.hasNext();) {
                if (iter.next().usesPlugin(plugin)) {
                    iter.remove();
                }
            }
        }
    }

    @Override
    public void enable() {
        plugin = this;

        // Pre-enable check
        if (!Common.hasCapability("Common:CommonItemStack")) {
            throw new UnsupportedOperationException("This version of MyWorlds requires BKCommonLib 1.21 or later");
        }

        // Event registering
        this.worldDupingPatch.enable(this);
        this.register(listener);
        this.register(new MWListenerPost(this));
        this.register("tpp", "world");
        this.register(this.migrator);

        // Soft Dependency evaluation beforehands
        isSpoutPluginEnabled = CommonUtil.isPluginEnabled("Spout");
        isMultiverseEnabled = CommonUtil.isPluginEnabled(MULTIVERSE_NAME);

        // Initialize the portal teleport cooldown logic
        portalTeleportationCooldown.enable();
        entityStasisHandler.enable();
        endRespawnHandler.enable();
        advancementManager.enable();

        // Load configurations, all below depends on these values too
        loadConfig();

        // Start automatic cleanup of portals we haven't been visited in a while
        netherPortalSearcher.enable();

        // World configurations have to be loaded first
        WorldConfig.init();

        // Portals
        this.portalSignList.enable();

        // World inventories
        WorldInventory.load();

        // Ensure mythic dungeons are setup correctly
        // Is automatically done when new worlds load in
        SoftDependency.detectAll(this);
        for (WorldConfig world : new ArrayList<>(WorldConfig.all())) {
            world.detectMythicDungeonsInstance();
        }

        // Fire portal enter events (debounced)
        portalEnterEventDebouncer.enable();

        // Player data controller
        dataController = new MWPlayerDataController(this);
        dataController.enable();

        // Auto-save every 15 minutes
        autoSaveTask.start(15*60*20, 15*60*20);
    }

    @Override
    public void disable() {
        // Portals
        this.portalSignList.disable();

        // Stop this
        portalEnterEventDebouncer.disable();
        netherPortalSearcher.disable();
        portalTeleportationCooldown.disable();
        entityStasisHandler.disable();
        endRespawnHandler.disable();
        setPAPIIntegrationEnabled(false, true);

        // Stop auto-saving
        autoSaveTask.stop();

        // World inventories
        // Now done for every change / command
        //WorldInventory.save();

        // World configurations have to be cleared last
        WorldConfig.deinit();

        // Abort chunk loader
        LoadChunksTask.abort(true);

        // Make sure to save all players before disabling - this prevents lost state
        for (Player player : Bukkit.getOnlinePlayers()) {
            CommonUtil.savePlayer(player);
        }

        // Detach data controller. Only do so when reloading, do not do it when
        // shutting down so we handle player saving correctly.
        if (!CommonUtil.isShuttingDown() && dataController != null) {
            dataController.detach();
            dataController = null;
        }

        // Wait until inventory migration is done
        migrator.waitUntilFinished();

        this.worldDupingPatch.disable();
        plugin = null;
    }

    public void loadConfig() {
        FileConfiguration config = new FileConfiguration(this);
        config.load();

        config.setHeader("This is the configuration of MyWorlds");
        config.addHeader("For more information, you can visit the following websites:");
        config.addHeader("https://www.spigotmc.org/resources/myworlds.8011/");
        config.addHeader("https://www.spigotmc.org/threads/myworlds.70876/");

        config.setHeader("teleportInterval", "\nThe interval in miliseconds a player has to wait before being teleported again");
        teleportInterval = config.get("teleportInterval", 2000);

        config.setHeader("timeLockInterval", "\nThe tick interval at which time is kept locked");
        timeLockInterval = config.get("timeLockInterval", 20);

        config.setHeader("useWorldInventories", "\nWhether or not world inventories are being separated using the settings");
        useWorldInventories = config.get("useWorldInventories", false);

        boolean hasStoreInvInMainWorldConfig = config.contains("storeInventoryInMainWorld");
        config.setHeader("storeInventoryInMainWorld", "\nWhether the player inventories are stored on the MyWorlds-configured main world");
        config.addHeader("storeInventoryInMainWorld", "When false, the Vanilla main world ('world') is used instead");
        config.addHeader("storeInventoryInMainWorld", "This option is also active when useWorldInventories is false");
        if (!hasStoreInvInMainWorldConfig) {
            if (!config.contains("mainWorld") && config.get("mainWorld", "").isEmpty()) {
                config.set("storeInventoryInMainWorld", false);
            } else {
                // Must maintain this option, otherwise inventories will break for everyone
                config.set("storeInventoryInMainWorld", true);
            }
        }
        storeInventoryInMainWorld = config.get("storeInventoryInMainWorld", false);

        useWorldEnterPermissions = config.get("useWorldEnterPermissions", false);
        useWorldBuildPermissions = config.get("useWorldBuildPermissions", false);
        useWorldUsePermissions = config.get("useWorldUsePermissions", false);
        useWorldChatPermissions = config.get("useWorldChatPermissions", false);
        if (useWorldChatPermissions && chatListener == null) {
            this.register(chatListener = new MWPlayerChatListener());
        } else if (!useWorldChatPermissions && chatListener != null) {
            CommonUtil.unregisterListener(chatListener);
            chatListener = null;
        }

        config.setHeader("keepLastPositionPermissionEnabled", "\nWhether players with the myworlds.world.keeplastpos permission are teleported to");
        config.addHeader("keepLastPositionPermissionEnabled", "the last position they had on a world when using /tpp or using portals");
        keepLastPositionPermissionEnabled = config.get("keepLastPositionPermissionEnabled", false);

        config.setHeader("keepInventoryPermissionEnabled", "\nWhether players with the myworlds.world.keepinventory permission keep their inventory");
        config.addHeader("keepInventoryPermissionEnabled", "unchanged when they teleport between worlds. This disables world inventory splitting logic for them.");
        keepInventoryPermissionEnabled = config.get("keepInventoryPermissionEnabled", false);

        config.setHeader("allowPortalNameOverride", "\nWhether portals can be replaced by other portals with the same name on the same world");
        allowPortalNameOverride = config.get("allowPortalNameOverride", true);

        config.setHeader("useWorldOperators", "\nWhether each world has it's own operator list");
        useWorldOperators = config.get("useWorldOperators", false);

        config.setHeader("onlyObsidianPortals", "\nWhether only portal blocks surrounded by obsidian can teleport players");
        onlyObsidianPortals = config.get("onlyObsidianPortals", false);

        config.setHeader("calculateWorldSize", "\nWhether the world info command will calculate the world size on disk");
        config.addHeader("calculateWorldSize", "If this process takes too long, disable it to prevent possible server freezes");
        calculateWorldSize = config.get("calculateWorldSize", true);

        config.setHeader("mainWorld", "\nThe main world in which new players spawn");
        config.addHeader("mainWorld", "If left empty, the main world defined in the server.properties is used");
        mainWorld = config.get("mainWorld", "");

        config.setHeader("forceMainWorldSpawn", "\nWhether all players respawn on the main world at all times");
        config.addHeader("forceMainWorldSpawn", "If this should only happen when players join the server, change forceJoinOnMainWorld instead");
        forceMainWorldSpawn = config.get("forceMainWorldSpawn", false);

        config.setHeader("forceJoinOnMainWorld", "\nWhether all players that join the server spawn on the main world");
        config.addHeader("forceJoinOnMainWorld", "This includes players that joined before. Respawns are not affected.");
        forceJoinOnMainWorld = config.get("forceJoinOnMainWorld", false) || forceMainWorldSpawn;

        config.setHeader("forceGamemodeChanges", "\nWhether the world game mode is applied to all players, even those with the");
        config.addHeader("forceGamemodeChanges", "myworlds.world.ignoregamemode permission");
        forceGamemodeChanges = config.get("forceGamemodeChanges", false);

        config.setHeader("alwaysInstantPortal", "\nWhether survival players instantly teleport when entering a nether portal");
        alwaysInstantPortal = config.get("alwaysInstantPortal", false);

        config.setHeader("maxPortalSignDistance", "\nThe maximum distance to look for a portal sign when entering a portal");
        maxPortalSignDistance = config.get("maxPortalSignDistance", 5.0);

        config.setHeader("enabledPortals", "\nTurns different types of portals on or off");
        config.addHeader("enabledPortals", "When the portal is disabled, MyWorlds will not handle the portal's logic");

        ConfigurationNode enabledPortals = config.getNode("enabledPortals");
        enabledPortals.setHeader("netherPortal", "Turns handling of nether portal teleportation on or off");
        enabledPortals.addHeader("netherPortal", "Vanilla Minecraft will handle nether portals when disabled");
        enabledPortals.setHeader("endPortal", "Turns handling of end portal teleportation on or off");
        enabledPortals.addHeader("endPortal", "Vanilla Minecraft will handle end portals when disabled");
        enabledPortals.setHeader("waterPortal", "Enables or disables the water stream portals");
        enabledPortals.setHeader("waterPortalStrict", "If water portal is enabled, true requires an exact water portal frame");
        enabledPortals.addHeader("waterPortalStrict", "With false, any transition air->water also detects nearby portal signs and teleports players");

        // Transfer legacy options
        if (config.contains("useWaterTeleport")) {
            enabledPortals.set("waterPortal", config.get("useWaterTeleport", true));
            config.remove("useWaterTeleport");
        }
        if (config.contains("enablePortals")) {
            if (!config.get("enablePortals", true)) {
                enabledPortals.set("netherPortal", false);
                enabledPortals.set("endPortal", false);
                enabledPortals.set("waterPortal", false);
            }
            config.remove("enablePortals");
        }

        netherPortalEnabled = enabledPortals.get("netherPortal", true);
        endPortalEnabled = enabledPortals.get("endPortal", true);
        waterPortalEnabled = enabledPortals.get("waterPortal", true);
        waterPortalStrict = enabledPortals.get("waterPortalStrict", true);

        config.setHeader("ignoreEggSpawns", "\nWhether egg-spawned entities are allowed to spawn, even if worlds have these");
        config.addHeader("entities blacklisted to be spawned");
        ignoreEggSpawns = config.get("ignoreEggSpawns", true);

        config.setHeader("ignoreBreedingSpawns", "\nWhether mob breeding and slime splitting is allowed to occur, even if worlds");
        config.addHeader("have these entities blacklisted to be spawned");
        ignoreBreedingSpawns = config.get("ignoreBreedingSpawns", false);

        config.setHeader("overridePortalPhysics", "\nWhether Vanilla portal physics are overrided to allow them to be built/stacked");
        overridePortalPhysics = config.get("overridePortalPhysics", true);

        ConfigurationNode portalSearchMatchRadiusNode = config.getNode("portalSearchMatchRadius");
        portalSearchMatchRadiusNode.setHeader("\nMyWorlds caches destinations for portals frequently entered to minimize lag");
        portalSearchMatchRadiusNode.addHeader("When a portal block is activated near to a previously activated one, search results are reused");
        portalSearchMatchRadiusNode.addHeader("The width and height control the sensitivity of matching these results");
        portalSearchMatchRadiusNode.addHeader("If a wrong portal is entered because of this, you can lower these values to fix that");
        portalSearchMatchRadius = new IntVector3(
                portalSearchMatchRadiusNode.get("width", 32),
                portalSearchMatchRadiusNode.get("height", 4),
                portalSearchMatchRadiusNode.get("width", 32));

        config.setHeader("importFromMultiVerse", "\nWhether to automatically import the world configuration of MultiVerse for new (unknown) worlds");
        config.addHeader("importFromMultiverse", "Note that default world properties are then no longer applied, as MultiVerse takes that over");
        config.addHeader("importFromMultiverse", "This setting is only active if MultiVerse-Core is installed.");
        importFromMultiVerse = config.get("importFromMultiVerse", true);

        config.setHeader("debugLogGMChanges", "\nWhether game mode changes are logged to console, including plugin name and stack trace");
        config.addHeader("debugLogGMChanges", "This helps to debug problems where game modes spuriously change, or fail to change properly");
        debugLogGMChanges = config.get("debugLogGMChanges", false);

        config.setHeader("portalToLastPosition", "\nWhether players are teleported to their last-known position on the world when they take a portal");
        config.addHeader("portalToLastPosition", "This is only active when 'remember last position' is enabled for the world");
        config.addHeader("portalToLastPosition", "It makes that option work not just for /tpp, but also when taking portals to a world");
        portalToLastPosition = config.get("portalToLastPosition", true);

        config.setHeader("portalSignsTeleportMobs", "\nWhether [portal] signs can teleport non-player entities (mobs and items)");
        config.addHeader("portalSignsTeleportMobs", "If true, items and mobs can teleport using portals near [portal] signs");
        config.addHeader("portalSignsTeleportMobs", "This option can be separately set per portal using /mw setportaloption (playersonly)");
        portalSignsTeleportMobs = config.get("portalSignsTeleportMobs", true);

        config.setHeader("worldTime24Hours", "\nWhether /world time and %myworlds_world_time% PAPI variable use 24 hours time format");
        config.addHeader("worldTime24Hours", "If true, shows format 23:45, if false, shows format 11:45 pm");
        worldTime24Hours = config.get("worldTime24Hours", true);

        config.save();
    }

    @Override
    public boolean command(CommandSender sender, String cmdLabel, String[] args) {
        commands.execute(sender, cmdLabel, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!Permission.SUGGESTIONS.has(sender)) {
            return Collections.emptyList();
        }

        return commands.autocomplete(sender, alias, args);
    }

    @Override
    public void localization() {
        this.loadLocales(Localization.class);
    }

    @Override
    public void permissions() {
        this.loadPermissions(Permission.class);
    }

    /**
     * Gets an instance of the My Worlds implementation of the Player Data Controller
     * 
     * @return MW Player Data Controller
     */
    public MWPlayerDataController getPlayerDataController() {
        return dataController;
    }

    /**
     * Prevents a just-initialized world from loading the spawn area
     * 
     * @param worldname to disable the spawn loading for during initialization
     */
    public void initDisableSpawn(String worldname) {
        spawnDisabledWorlds.add(worldname);
    }

    /**
     * Clears the init-disable spawn for a world, and returns whether it was disabled
     * 
     * @param worldname to clear
     * @return True if spawn was disabled, False if not
     */
    public boolean clearInitDisableSpawn(String worldname) {
        return spawnDisabledWorlds.remove(worldname);
    }

    /**
     * Changes the main world used. Writes new main world to config.
     *
     * @param newMainWorldName
     */
    public void changeMainWorld(String newMainWorldName) {
        mainWorld = newMainWorldName;
        storeInventoryInMainWorld = true;

        FileConfiguration config = new FileConfiguration(this);
        config.load();
        config.set("storeInventoryInMainWorld", true);
        config.set("mainWorld", newMainWorldName);
        config.save();
    }

    /**
     * Turns on or off the per-world inventories
     *
     * @param enabled
     */
    public void setUseWorldInventories(boolean enabled) {
        if (MyWorlds.useWorldInventories != enabled) {
            MyWorlds.useWorldInventories = enabled;
            FileConfiguration config = new FileConfiguration(this);
            config.load();
            config.set("useWorldInventories", enabled);
            config.save();
        }
    }

    /**
     * Checks whether teleportation handling of a given Portal block should be ignored
     *
     * @param portalType
     * @param portalBlock
     * @return True if portal is filtered/ignored
     */
    public boolean isPortalFiltered(PortalType portalType, Block portalBlock) {
        for (PortalFilter filter : this.portalFilters) {
            if (filter.isPortalFiltered(portalType, portalBlock)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the main world
     * 
     * @return Main world
     */
    public static World getMainWorld() {
        if (!mainWorld.isEmpty()) {
            World world = Bukkit.getWorld(mainWorld);
            if (world != null) {
                return world;
            }
        }
        return WorldUtil.getWorlds().iterator().next();
    }

    private void setPAPIIntegrationEnabled(boolean enabled, boolean isShutdown) {
        if (enabled && placeholderApi == null) {
            try {
                placeholderApi = new PlaceholderAPIHandlerWithExpansions(this);
                placeholderApi.enable();
                getLogger().log(Level.INFO, "PlaceholderAPI integration enabled");
            } catch (Throwable t) {
                getLogger().log(Level.SEVERE, "Failed to disable PlaceholderAPI integration", t);
            }
        } else if (!enabled && placeholderApi != null) {
            try {
                placeholderApi.disable();
                if (!isShutdown) {
                    getLogger().log(Level.INFO, "PlaceholderAPI integration disabled");
                }
            } catch (Throwable t) {
                getLogger().log(Level.SEVERE, "Failed to disable PlaceholderAPI integration", t);
            }
            placeholderApi = null;
        }
    }

    private static class AutoSaveTask extends Task {

        public AutoSaveTask(JavaPlugin plugin) {
            super(plugin);
        }

        @Override
        public void run() {
            WorldConfigStore.saveAll();
        }
    }
}
