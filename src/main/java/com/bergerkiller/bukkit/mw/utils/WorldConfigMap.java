package com.bergerkiller.bukkit.mw.utils;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.collections.StringMapCaseInsensitive;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;
import com.bergerkiller.bukkit.mw.WorldMode;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * HashMap by world name (case-insensitive) and loaded world instance.
 * Is multithread-safe.
 * The WorldConfig store uses this for all its APIs, and this is here
 * to make sure it works asynchronously properly.
 */
public abstract class WorldConfigMap {
    // These must be updated while synchronized
    private final StringMapCaseInsensitive<WorldConfig> worldConfigsByName = new StringMapCaseInsensitive<>();
    private final IdentityHashMap<World, WorldConfig> worldConfigsByWorld = new IdentityHashMap<>();
    // These are copies and can be read asynchronously
    private IdentityHashMap<World, WorldConfig> worldConfigsByWorldCopy = new IdentityHashMap<>();
    private List<WorldConfig> worldConfigValues = Collections.emptyList();
    // New WorldConfig values that have been added for new worlds for which we have not yet applied configurations
    // This applying must occur on the main thread, so a task is dispatched to do it later if needed
    private final Set<WorldConfig> newWorldConfigPendingApply = new HashSet<>();

    protected abstract WorldConfig createNewConfiguration(MyWorlds plugin, String worldName, World world);

    protected abstract void initializeValue(MyWorlds plugin, WorldConfig config, World world, WorldMode worldmode, boolean doInitialization);

    /**
     * Called on main thread to apply a world configuration to a world
     *
     * @param config WorldConfig
     */
    protected abstract void applyConfiguration(WorldConfig config);

    private void store(MyWorlds plugin, String worldName, World world, WorldMode worldmode, boolean doInitialization) {
        WorldConfig value;
        synchronized (this) {
            value = worldConfigsByName.get(worldName);
            if (value != null) {
                addToByBukkitWorldMap(world, value);
                return;
            }

            if (world != null) {
                worldName = world.getName();
            }

            value = createNewConfiguration(plugin, worldName, world);
            worldConfigsByName.put(worldName, value);

            {
                List<WorldConfig> newValues = new ArrayList<>(worldConfigValues);
                newValues.add(value);
                worldConfigValues = newValues;
            }

            addToByBukkitWorldMap(world, value);

            if (doInitialization) {
                newWorldConfigPendingApply.add(value);
            }
        }

        try {
            initializeValue(plugin, value, world, worldmode, doInitialization);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize world configuration for " + worldName, t);
        }

        // If main thread, apply world config stuff right away otherwise do it next-tick
        if (doInitialization) {
            if (CommonUtil.isMainThread()) {
                applyWorldConfigurations();
            } else {
                new Task(plugin) {
                    @Override
                    public void run() {
                        applyWorldConfigurations();
                    }
                }.start();
            }
        }
    }

    public void applyWorldConfigurations() {
        while (true) {
            WorldConfig config;
            synchronized (this) {
                Iterator<WorldConfig> iter = newWorldConfigPendingApply.iterator();
                if (!iter.hasNext()) {
                    break;
                }

                config = iter.next();
                iter.remove();
            }

            try {
                applyConfiguration(config);
            } catch (Throwable t) {
                config.getPlugin().getLogger().log(Level.SEVERE,
                        "Failed to apply world configuration to " + config.worldname, t);
            }
        }
    }

    private synchronized void addToByBukkitWorldMap(World world, WorldConfig value) {
        if (world != null) {
            worldConfigsByWorld.put(world, value);
            worldConfigsByWorldCopy = new IdentityHashMap<>(worldConfigsByWorld);
        }
    }

    public List<WorldConfig> all() {
        return worldConfigValues;
    }

    public synchronized WorldConfig getIfExists(String worldName) {
        return worldConfigsByName.get(worldName);
    }

    public WorldConfig getIfExists(World world) {
        WorldConfig value = worldConfigsByWorldCopy.get(world);
        if (value == null) {
            synchronized (this) {
                value = worldConfigsByWorld.get(world);
                if (value == null) {
                    value = worldConfigsByName.get(world.getName());
                    if (value != null) {
                        addToByBukkitWorldMap(world, value);
                    }
                }
            }
        }

        return value;
    }

    public synchronized WorldConfig create(MyWorlds plugin, String worldName, WorldMode worldmode, boolean doInitialization) {
        WorldConfig value = worldConfigsByName.get(worldName);
        if (value != null) {
            return value;
        }

        World world = WorldManager.getWorld(worldName);
        store(plugin, worldName, world, worldmode, doInitialization);
        value = worldConfigsByName.get(worldName);
        if (value == null) {
            throw new IllegalStateException("Could not initialize value for world " + worldName);
        }

        return value;
    }

    public WorldConfig create(MyWorlds plugin, World world, WorldMode worldmode, boolean doInitialization) {
        if (world == null) {
            return null;
        }

        WorldConfig value = worldConfigsByWorldCopy.get(world);
        if (value == null) {
            store(plugin, world.getName(), world, worldmode, doInitialization);
            value = worldConfigsByWorldCopy.get(world);
            if (value == null) {
                throw new IllegalStateException("Could not initialize value for world " + world.getName());
            }
        }
        return value;
    }

    public synchronized void resetByBukkitWorldMap() {
        worldConfigsByWorld.clear();
        worldConfigsByWorldCopy = new IdentityHashMap<>();
    }

    public synchronized void clear() {
        worldConfigsByName.clear();
        worldConfigValues = Collections.emptyList();
        resetByBukkitWorldMap();
    }

    public synchronized void removeFromByBukkitWorldLookup(World world) {
        if (world == null) {
            return;
        }

        if (worldConfigsByWorld.remove(world) != null) {
            worldConfigsByWorldCopy = new IdentityHashMap<>(worldConfigsByWorld);
        }
    }

    public synchronized WorldConfig remove(World world) {
        if (world == null) {
            return null;
        }

        WorldConfig config = remove(world.getName());
        removeFromByBukkitWorldLookup(world);
        return config;
    }

    public synchronized WorldConfig remove(String worldName) {
        // Remove from by-name mapping
        WorldConfig config = worldConfigsByName.remove(worldName);
        worldConfigValues = removeValue(worldConfigValues, config);
        // Remove all by-world mappings by this name
        for (Iterator<Map.Entry<World, WorldConfig>> iter = worldConfigsByWorld.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<World, WorldConfig> e = iter.next();
            if (!(e.getKey().getName().equalsIgnoreCase(worldName))) {
                continue;
            }

            worldConfigValues = removeValue(worldConfigValues, e.getValue());
            iter.remove();
        }
        worldConfigsByWorldCopy = new IdentityHashMap<>(worldConfigsByWorld);
        return config;
    }

    private static <V> List<V> removeValue(List<V> list, V value) {
        List<V> result = new ArrayList<>(list);
        if (result.remove(value)) {
            return result;
        } else {
            return list;
        }
    }
}
