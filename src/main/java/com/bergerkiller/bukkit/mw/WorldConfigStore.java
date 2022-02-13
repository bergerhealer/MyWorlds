package com.bergerkiller.bukkit.mw;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.logging.Level;

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

    private static WorldConfig create(String worldname) {
        WorldConfig wc = new WorldConfig(worldname);
        worldConfigs.put(wc.worldname, wc);
        wc.loadDefaults();
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
     * @param worldname to get the configuration of
     * @param worldmode to force, use null to use the current
     * @return World Config of the world
     */
    public static WorldConfig get(String worldname, WorldMode worldmode) {
        WorldConfig c = worldConfigs.get(worldname);
        if (c == null) {
            c = create(worldname);
            if (worldmode != null) {
                c.worldmode = worldmode;
            }
            c.reset();
        } else if (worldmode != null) {
            c.worldmode = worldmode;
        }
        return c;
    }

    public static WorldConfig get(String worldname) {
        return get(worldname, null);
    }

    public static WorldConfig get(World world) {
        return worldConfigsByWorld.computeIfAbsent(world, w -> get(w.getName()));
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

    public static Collection<WorldConfig> all() {
        return worldConfigs.values();
    }

    public static boolean exists(String worldname) {
        return worldConfigs.containsKey(worldname);
    }

    public static void init() {
        // Default configuration
        defaultProperties = new FileConfiguration(MyWorlds.plugin, "defaultproperties.yml");
        defaultProperties.setHeader("This file contains the default world properties applied when loading or creating completely new worlds");
        defaultProperties.addHeader("All the nodes found in the worlds.yml can be set here");
        defaultProperties.addHeader("To set environment/worldtype-specific settings, add a new node with this name");
        if (defaultProperties.exists()) {
            defaultProperties.load();
        } else {
            // Generate new properties
            WorldConfig defConfig = new WorldConfig(null);
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
        FileConfiguration config = new FileConfiguration(MyWorlds.plugin, "worlds.yml");
        config.load();
        for (ConfigurationNode node : config.getNodes()) {
            String worldname = node.get("name", node.getName());
            if (WorldManager.worldExists(worldname)) {
                WorldConfig wc = create(worldname);
                wc.load(node);
                if (node.get("loaded", false)) {
                    wc.loadWorld();
                }
            } else {
                MyWorlds.plugin.log(Level.WARNING, "World: " + node.getName() + " no longer exists, data will be wiped when disabling!");
            }
        }
        // For any new worlds that are made available: generate a configuration here
        for (String loadableWorld : WorldUtil.getLoadableWorlds()) {
            get(loadableWorld);
        }

        // Update any remaining worlds
        for (World world : WorldUtil.getWorlds()) {
            get(world).onWorldLoad(world);
        }
    }

    public static void saveAll() {
        FileConfiguration cfg = new FileConfiguration(MyWorlds.plugin, "worlds.yml");
        for (WorldConfig wc : all()) {
            if (wc.isExisting()) {
                wc.save(cfg.getNode(wc.getConfigName()));
            }
        }
        cfg.save();

        // Clean up the cache by bukkit world - is repopulated automatically
        worldConfigsByWorld.clear();
    }

    public static void deinit() {
        // Tell all loaded worlds to unload (for MyWorlds) to properly handle disabling
        for (World world : WorldUtil.getWorlds()) {
            get(world).onWorldUnload(world);
        }
        // Save the current world configurations
        saveAll();
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
        worldConfigs.remove(worldname);
        // Remove references to this World Configuration in other worlds
        for (WorldConfig otherConfig : all()) {
            if (usesWorldAsDestination(worldname, otherConfig.getDefaultNetherPortalDestination())) {
                otherConfig.setDefaultNetherPortalDestination(null);
            }
            if (usesWorldAsDestination(worldname, otherConfig.getDefaultEndPortalDestination())) {
                otherConfig.setDefaultEndPortalDestination(null);
            }
            otherConfig.inventory.remove(worldname, false);
        }
    }

    private static boolean usesWorldAsDestination(String worldname, PortalDestination destination) {
        return destination != null && destination.getName() != null && destination.getName().equals(worldname);
    }
}
