package com.bergerkiller.bukkit.mw;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.logging.Level;

import com.bergerkiller.bukkit.common.Task;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import com.bergerkiller.bukkit.common.collections.StringMapCaseInsensitive;
import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.config.FileConfiguration;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.mw.portal.PortalDestination;

public class WorldConfigStore {
    private static StringMapCaseInsensitive<WorldConfig> worldConfigs = new StringMapCaseInsensitive<WorldConfig>();
    private static IdentityHashMap<World, WorldConfig> worldConfigsByWorld = new IdentityHashMap<>();
    private static FileConfiguration defaultProperties;
    private static boolean initializing = false;
    private static Task fastAutoSaveTask = null;

    private static WorldConfig create(MyWorlds plugin, String worldname, boolean assignToMatchedInventory) {
        WorldConfig wc = new WorldConfig(plugin, worldname);
        worldConfigs.put(wc.worldname, wc);
        wc.loadDefaults(assignToMatchedInventory);
        saveAllLater(); // Save new world configs sooner
        return wc;
    }

    protected static void purgeWorldFromByWorldLookup(World world) {
        worldConfigsByWorld.remove(world);
    }

    /**
     * Gets the World Configuration of a world, if it exists presently.
     * Returns null if it does not exist.
     * 
     * @param worldname
     * @return World Config of the world, null if not found
     */
    public static WorldConfig getIfExists(String worldname) {
        return worldConfigs.get(worldname);
    }

    /**
     * Gets the World Configuration of a world, while forcing a particular environment.
     *
     * @param worldName to get the configuration of
     * @param worldMode to force, use null to use the current
     * @return World Config of the world
     */
    public static WorldConfig get(String worldName, WorldMode worldMode) {
        return get(MyWorlds.plugin, worldName, worldMode);
    }

    private static WorldConfig get(MyWorlds plugin, String worldname, WorldMode worldmode) {
        WorldConfig c = worldConfigs.get(worldname);
        if (c == null) {
            c = create(plugin, worldname, true);
            if (worldmode != null) {
                c.worldmode = worldmode;
            }
            c.reset();
            c.detectGeneratorDisableAutoLoad();
        } else if (worldmode != null) {
            c.worldmode = worldmode;
        }
        return c;
    }

    public static WorldConfig get(String worldName) {
        return get(worldName, null);
    }

    public static WorldConfig get(World world) {
        return get(MyWorlds.plugin, world);
    }

    private static WorldConfig get(MyWorlds plugin, World world) {
        return worldConfigsByWorld.computeIfAbsent(world, w -> get(plugin, w.getName(), null));
    }

    public static WorldConfig get(Entity entity) {
        return get(entity.getWorld());
    }

    public static WorldConfig get(Location location) {
        return get(location.getWorld());
    }

    public static WorldConfig get(Block block) {
        return get(block.getWorld());
    }

    /**
     * Gets the World Configuration of the main world
     * 
     * @return Main world configuration
     */
    public static WorldConfig getMain() {
        return get(MyWorlds.getMainWorld());
    }

    /**
     * Gets the World Configuration of the Vanilla main world, which
     * is normally called 'world'.
     * 
     * @return Vanilla main world configuration
     */
    public static WorldConfig getVanillaMain() {
        return get(Bukkit.getWorlds().get(0));
    }

    /**
     * Gets the World Configuration of the main world for inventory storage. This is
     * the first world where player data is loaded when players join to figure out what
     * world they are on. This world also stores special metadata, like the last positions
     * players had.
     *
     * @return Inventory main world configuration
     */
    public static WorldConfig getInventoryMain() {
        return MyWorlds.storeInventoryInMainWorld ? getMain() : getVanillaMain();
    }

    public static Collection<WorldConfig> all() {
        return worldConfigs.values();
    }

    /**
     * Looks up a main world config that has a rejoin group configuration that includes
     * the world specified.
     *
     * @param world World part of a rejoin group
     * @return WorldConfig that has a rejoinGroup that includes the world specified,
     *         or the WorldConfig of the world itself if none exist.
     */
    public static WorldConfig findRejoin(WorldConfig world) {
        // Most common case: main world is itself a group
        if (world.rejoinGroup.isEmpty()) {
            // See if there's another world that contains it
            for (WorldConfig otherConfig : all() ) {
                if (world != otherConfig && otherConfig.rejoinGroup.contains(world.worldname)) {
                    return otherConfig;
                }
            }
        }
        return world;
    }

    public static boolean exists(String worldname) {
        return worldConfigs.containsKey(worldname);
    }

    /**
     * Performs both the {@link #initLoadConfig(MyWorlds)} and {@link #initStartup(MyWorlds)}
     * phases of initialization. Primarily useful to perform a reload of world configuration from disk.
     */
    public static void init() {
        initLoadConfig(MyWorlds.plugin);
        initStartup(MyWorlds.plugin);
    }

    /**
     * (Re-)loads the full worlds.yml and defaultproperties.yml world configurations from disk.
     * Does not yet apply this information to the world, this is done during the
     * {@link #initStartup(MyWorlds)} phase.
     *
     * @param plugin MyWorlds plugin instance
     */
    public static void initLoadConfig(MyWorlds plugin) {
        initializing = true;
        boolean isNewConfig;
        try {
            // Default configuration
            defaultProperties = new FileConfiguration(plugin, "defaultproperties.yml");
            defaultProperties.setHeader("This file contains the default world properties applied when loading or creating completely new worlds");
            defaultProperties.addHeader("All the nodes found in the worlds.yml can be set here");
            defaultProperties.addHeader("To set environment/worldtype-specific settings, add a new node with this name");
            if (defaultProperties.exists()) {
                defaultProperties.load();
            } else {
                // Generate new properties
                WorldConfig defConfig = new WorldConfig(plugin, null);
                defConfig.gameMode = null;
                defConfig.saveDefault(defaultProperties);
                ConfigurationNode defEnv = defaultProperties.getNode("normal");
                defEnv.set("gamemode", "NONE");
                defEnv.setHeader("\nAll settings applied to worlds with the normal environment");
                defEnv.addHeader("You can add all the same world settings here and they will override the main defaults");
                defEnv.addHeader("You can use multiple environments, of which nether, the_end and even nether_flat");
                defaultProperties.save();
            }

            // Worlds configuration
            worldConfigs.clear();
            worldConfigsByWorld.clear();
            FileConfiguration config = new FileConfiguration(plugin, "worlds.yml");
            isNewConfig = !config.exists();
            config.load();
            for (ConfigurationNode node : config.getNodes()) {
                String worldName = node.get("name", node.getName());
                if (WorldManager.worldExists(worldName)) {
                    WorldConfig wc = create(plugin, worldName, false);
                    wc.load(node);
                } else {
                    plugin.log(Level.WARNING, "World: " + worldName + " no longer exists, data will be wiped when disabling!");
                }
            }

            // For any new worlds that are made available: generate a configuration here
            for (String loadableWorld : WorldUtil.getLoadableWorlds()) {
                get(plugin, loadableWorld, null);
            }

            // Probably dead code but just in case - ensure all loaded worlds have a configuration already
            for (World world : WorldUtil.getWorlds()) {
                get(plugin, world);
            }
        } finally {
            initializing = false;
        }

        // If no worlds.yml existed yet, generate it
        if (isNewConfig) {
            saveAll(plugin);
        }
    }

    /**
     * Plugin enable initialization step. At this point the previously loaded configuration is applied to
     * loaded worlds, and worlds set to load on startup are loaded.
     *
     * @param plugin MyWorlds plugin instance
     */
    public static void initStartup(MyWorlds plugin) {
        // Ensure all currently loaded worlds have a configuration, generate a new one if needed
        // Fire an initial onWorldLoad event for them
        for (World world : WorldUtil.getWorlds()) {
            get(world).onWorldLoad(world);
        }

        // For world configurations set to load on startup, load them
        for (WorldConfig wc : new ArrayList<>(WorldConfig.all())) {
            wc.detectGeneratorDisableAutoLoad();
            if (wc.getStartupLoadMode() == WorldStartupLoadMode.LOADED) {
                wc.loadWorld();
            }
        }
    }

    /**
     * Saves the worlds.yml in the next tick. Debounces autosaves after big changes happen.
     */
    public static void saveAllLater() {
        if (initializing || MyWorlds.plugin == null) {
            return;
        }

        if (fastAutoSaveTask == null && MyWorlds.plugin.isEnabled()) {
            fastAutoSaveTask = new Task(MyWorlds.plugin) {
                @Override
                public void run() {
                    fastAutoSaveTask = null;
                    saveAll(MyWorlds.plugin);
                }
            }.start();
        }
    }

    @Deprecated
    public static void saveAll() {
        saveAll(MyWorlds.plugin);
    }

    public static void saveAll(MyWorlds plugin) {
        // Cancel fast auto save
        if (fastAutoSaveTask != null) {
            fastAutoSaveTask.stop();
            fastAutoSaveTask = null;
        }

        // Do NOT do any saving while initializing the configuration
        // This causes a loss of state
        if (initializing) {
            return;
        }

        FileConfiguration cfg = new FileConfiguration(plugin, "worlds.yml");
        for (WorldConfig wc : all()) {
            if (wc.isExisting()) {
                wc.save(cfg.getNode(wc.getConfigName()));
            }
        }
        cfg.save();

        // Clean up the cache by bukkit world - is repopulated automatically
        worldConfigsByWorld.clear();
    }

    public static void deinit(MyWorlds plugin) {
        // Tell all loaded worlds to unload (for MyWorlds) to properly handle disabling
        for (World world : WorldUtil.getWorlds()) {
            get(world).onWorldUnload(world, true);
        }
        // Save the current world configurations
        saveAll(plugin);
        // De-initialize some data
        defaultProperties = null;
    }

    /**
     * Gets the Default Properties configuration.A null return indicates that no defaults
     * are available, which can occur after MyWorlds disabled.
     * 
     * @return The default properties configuration, or null if unavailable
     */
    public static ConfigurationNode getDefaultProperties() {
        return defaultProperties == null ? null : defaultProperties.clone();
    }

    /**
     * Removes a specific World Configuration from this storage.
     * Please note that this method can NOT be used async.
     * 
     * @param worldname to remove
     */
    public static void remove(String worldname) {
        // Unregister the world configuration to remove it
        WorldConfig removedConfig = worldConfigs.remove(worldname);
        if (removedConfig != null) {
            removedConfig.inventory.remove(worldname); // In case of 1-world inventories
        }
        // Remove references to this World Configuration in other worlds
        for (WorldConfig otherConfig : all()) {
            clearDestinationIfUsed(otherConfig.getDefaultNetherPortalDestination(), worldname);
            clearDestinationIfUsed(otherConfig.getDefaultEndPortalDestination(), worldname);
            otherConfig.inventory.remove(worldname);
        }
    }

    private static void clearDestinationIfUsed(PortalDestination destination, String worldname) {
        if (destination != null && destination.getName() != null && destination.getName().equals(worldname)) {
            destination.setName("");
        }
    }
}
