package com.bergerkiller.bukkit.mw;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.config.FileConfiguration;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

public class WorldInventory {
    private static final Set<WorldInventory> inventories = new HashSet<WorldInventory>();
    private static int counter = 0;
    private final Set<String> worlds = new HashSet<String>();
    private String worldname;
    private String name;
    
    public static Collection<WorldInventory> getAll() {
        return inventories;
    }

    public static WorldInventory create(String worldName) {
        return new WorldInventory(worldName).add(worldName);
    }

    public static void load() {
        inventories.clear();
        FileConfiguration config = new FileConfiguration(MyWorlds.plugin, "inventories.yml");
        config.load();
        for (ConfigurationNode node : config.getNodes()) {
            String sharedWorld = node.get("folder", String.class, null);
            if (sharedWorld == null) {
                continue;
            }
            List<String> worlds = node.getList("worlds", String.class);
            if (worlds.isEmpty()) {
                continue;
            }
            WorldInventory inv = new WorldInventory(WorldConfig.get(sharedWorld).worldname);
            inv.name = node.getName();
            for (String world : worlds) {
                inv.addWithoutSaving(world);
            }
        }
    }

    public static void save() {
        FileConfiguration config = new FileConfiguration(MyWorlds.plugin, "inventories.yml");
        Set<String> savedNames = new HashSet<String>();
        for (WorldInventory inventory : inventories) {
            if (inventory.worlds.size() > 1) {
                String name = inventory.name;
                for (int i = 0; i < Integer.MAX_VALUE && !savedNames.add(name.toLowerCase()); i++) {
                    name = inventory.name + i;
                }
                ConfigurationNode node = config.getNode(name);
                node.set("folder", inventory.worldname);
                node.set("worlds", new ArrayList<String>(inventory.worlds));
            }
        }
        config.save();
    }

    public static void detach(Collection<String> worldnames) {
        if (!worldnames.isEmpty()) {
            for (String world : worldnames) {
                WorldConfig.get(world).inventory.removeWithoutSaving(world, true);
            }
            save();
        }
    }

    public static void merge(Collection<String> worldnames) {
        if (!worldnames.isEmpty()) {
            WorldInventory inv = new WorldInventory(null);
            for (String world : worldnames) {
                inv.addWithoutSaving(world);
            }
            save();
        }
    }

    private WorldInventory(String sharedWorldName) {
        inventories.add(this);
        this.name = "inv" + counter++;
        this.worldname = sharedWorldName;
    }

    public Collection<String> getWorlds() {
        return this.worlds;
    }

    /**
     * Gets the World name in which all the inventories of this bundle are saved
     * 
     * @return shared world name
     */
    public String getSharedWorldName() {
        if (this.worldname == null || !WorldUtil.getWorldFolder(this.worldname).exists()) {
            this.worldname = getSharedWorldName(this.worlds);
            if (this.worldname == null) {
                throw new RuntimeException("Unable to locate a valid World folder to use for player data");
            }
        }
        return this.worldname;
    }

    private static String getSharedWorldName(Collection<String> worlds) {
        for (String world : worlds) {
            if (WorldConfig.get(world).getWorldFolder().exists()) {
                return world;
            }
        }
        return null;
    }

    public boolean contains(String worldname) {
        return this.worlds.contains(worldname.toLowerCase());
    }

    public boolean remove(String worldname) {
        boolean result = removeWithoutSaving(worldname, false);
        if (result) {
            save();
        }
        return result;
    }

    private boolean removeWithoutSaving(String worldname, boolean createNew) {
        boolean removed = false;
        if (this.worlds.remove(worldname.toLowerCase())) {
            removed = true;

            //constructor handles world config update
            if (createNew) {
                new WorldInventory(worldname).addWithoutSaving(worldname);
            }
        }
        if (this.worlds.isEmpty()) {
            removed = true;
            inventories.remove(this);
        } else if (worldname.equalsIgnoreCase(this.worldname)) {
            removed = true;
            this.worldname = getSharedWorldName(this.worlds);
            if (this.worldname == null) {
                inventories.remove(this);
            }
        }
        return removed;
    }

    public WorldInventory add(String worldname) {
        WorldInventory inv = this.addWithoutSaving(worldname);
        save();
        return inv;
    }

    private WorldInventory addWithoutSaving(String worldname) {
        WorldConfig config = WorldConfig.get(worldname);
        if (config.inventory != null) {
            config.inventory.removeWithoutSaving(config.worldname, false);
        }
        config.inventory = this;
        this.worlds.add(worldname.toLowerCase());
        if (this.worldname == null) {
            this.worldname = getSharedWorldName(this.worlds);
        }
        return this;
    }
}
