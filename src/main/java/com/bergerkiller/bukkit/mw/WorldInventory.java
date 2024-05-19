package com.bergerkiller.bukkit.mw;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.config.FileConfiguration;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.common.wrappers.PlayerRespawnPoint;

public class WorldInventory {
    private static final Set<WorldInventory> inventories = new HashSet<WorldInventory>();
    private static final SortedMap<MatchRule, WorldInventory> inventoryByNamePattern = new TreeMap<>();
    private static boolean inventoriesLoaded = false;
    private static int counter = 0;
    private final Set<String> worlds = new HashSet<String>();
    private final Set<MatchRule> worldNameMatchRules = new HashSet<>();
    private String worldname;
    private String name;

    public static Collection<WorldInventory> getAll() {
        return inventories;
    }

    public static WorldInventory create(String worldName) {
        return new WorldInventory(worldName).add(worldName);
    }

    /**
     * Tries to see if any of the pre-existing inventories matches a world name pattern.
     * If it does, returns that one. Otherwise, returns a new empty world inventory
     * for this specific world name.
     *
     * @param worldName World name
     * @return WorldInventory with a match rule for it, or null otherwise
     */
    public static WorldInventory matchOrCreate(String worldName) {
        for (Map.Entry<MatchRule, WorldInventory> e : inventoryByNamePattern.entrySet()) {
            if (e.getKey().matches(worldName)) {
                return e.getValue();
            }
        }
        return new WorldInventory(worldName);
    }

    public static void load() {
        inventoriesLoaded = true;

        // Check whether there are any configured entries that would result in saving
        boolean hadExistingInventoriesThatRequiredSaving = false;
        for (WorldInventory inv : inventories) {
            if (inv.isRequiredSaving()) {
                hadExistingInventoriesThatRequiredSaving = true;
                break;
            }
        }

        // Load the new configuration. Replace found settings with already-generated ones.
        FileConfiguration config = new FileConfiguration(MyWorlds.plugin, "inventories.yml");
        config.load();
        for (ConfigurationNode node : config.getNodes()) {
            String sharedWorld = node.get("folder", String.class, null);
            if (sharedWorld == null) {
                continue;
            }

            List<String> worlds = node.getList("worlds", String.class);
            List<String> matches = node.getList("matches", String.class);
            if (worlds.isEmpty() && matches.isEmpty()) {
                continue;
            }

            WorldInventory inv = new WorldInventory(WorldConfig.get(sharedWorld).worldname);
            inv.name = node.getName();
            for (String matchExpression : matches) {
                inv.worldNameMatchRules.add(MatchRule.of(matchExpression));
            }
            for (String world : worlds) {
                // This assigns inv to WorldConfig. If a previous WorldConfig was set for a world,
                // that one is de-registered.
                inv.addWithoutSaving(world);
            }
        }

        // Rebuild global name matcher
        rebuildNamePatternMap();

        // Re-save after loading in case merging of previous default inventories caused changes
        if (hadExistingInventoriesThatRequiredSaving) {
            save();
        }
    }

    public static void save() {
        // Avoid overwriting inventories.yml with incomplete data before it is all loaded in
        if (!inventoriesLoaded) {
            return;
        }

        FileConfiguration config = new FileConfiguration(MyWorlds.plugin, "inventories.yml");
        Set<String> savedNames = new HashSet<String>();
        for (WorldInventory inventory : inventories) {
            if (inventory.isRequiredSaving()) {
                String name = inventory.name;
                for (int i = 0; i < Integer.MAX_VALUE && !savedNames.add(name.toLowerCase()); i++) {
                    name = inventory.name + i;
                }
                ConfigurationNode node = config.getNode(name);
                node.set("folder", inventory.worldname);
                node.set("worlds", new ArrayList<>(inventory.worlds));
                if (inventory.worldNameMatchRules.isEmpty()) {
                    node.remove("matches");
                } else {
                    ArrayList<String> expressions = new ArrayList<>(inventory.worldNameMatchRules.size());
                    for (MatchRule rule : inventory.worldNameMatchRules) {
                        expressions.add(rule.getExpression());
                    }
                    node.set("matches", expressions);
                }
            }
        }
        config.save();
    }

    private static void rebuildNamePatternMap() {
        inventoryByNamePattern.clear();
        for (WorldInventory inv : inventories) {
            for (MatchRule rule : inv.worldNameMatchRules) {
                inventoryByNamePattern.put(rule, inv);
            }
        }
    }

    public static void detach(Collection<String> worldnames) {
        // Collect all the loaded Bukkit worlds impacted by this
        Set<World> loadedWorlds = new HashSet<>();
        for (String world : worldnames) {
            for (String invworld : WorldConfig.get(world).inventory.getWorlds()) {
                World w = WorldConfig.get(invworld).getWorld();
                if (w != null) {
                    loadedWorlds.add(w);
                }
            }
        }

        // Modify
        if (!worldnames.isEmpty()) {
            for (String world : worldnames) {
                WorldConfig wc = WorldConfig.get(world);
                wc.inventory.removeWithoutSaving(world, true);
            }
            save();

            // Validate the bed spawn points of all worlds impacted, to make sure none of them
            // refer to a now-inaccessible world.
            for (World loadedWorld : loadedWorlds) {
                for (Player player : loadedWorld.getPlayers()) {
                    if (!MWPlayerDataController.isValidRespawnPoint(
                            loadedWorld,
                            PlayerRespawnPoint.forPlayer(player))
                    ) {
                        PlayerRespawnPoint.NONE.applyToPlayer(player);
                    }
                }
            }
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
     * Gets whether this inventory configuration must be written to inventories.yml.
     * Default single-world isolated confogurations don't need to be written out
     *
     * @return True if this entry must be saved for proper persistence
     */
    private boolean isRequiredSaving() {
        return !this.worldNameMatchRules.isEmpty() || this.worlds.size() > 1;
    }

    /**
     * Changes the shared world name where inventory data is stored. Should only be called
     * from inventory migration.
     *
     * @param worldName
     */
    public void setSharedWorldName(String worldName) {
        if (!this.worlds.contains(worldName.toLowerCase())) {
            throw new IllegalArgumentException("World name " + worldName + " is not part of this inventory group");
        }
        this.worldname = worldName;
        save();
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

    public boolean contains(World world) {
        return world != null && contains(world.getName());
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

    public WorldInventory add(WorldConfig worldConfig) {
        if (this.addWithoutSaving(worldConfig)) {
            save();
        }
        return this;
    }

    public WorldInventory add(String worldname) {
        if (this.addWithoutSaving(worldname)) {
            save();
        }
        return this;
    }

    public Set<MatchRule> getMatchRules() {
        return Collections.unmodifiableSet(this.worldNameMatchRules);
    }

    public WorldInventory addMatchRule(String expression) {
        if (this.worldNameMatchRules.add(MatchRule.of(expression))) {
            rebuildNamePatternMap();
            save();
        }
        return this;
    }

    public WorldInventory clearMatchRules() {
        if (!this.worldNameMatchRules.isEmpty()) {
            this.worldNameMatchRules.clear();
            rebuildNamePatternMap();
            save();
        }
        return this;
    }

    private boolean addWithoutSaving(String worldname) {
        return addWithoutSaving(WorldConfig.get(worldname));
    }

    private boolean addWithoutSaving(WorldConfig worldConfig) {
        if (worldConfig.inventory == this) {
            return false;
        }
        if (worldConfig.inventory != null) {
            worldConfig.inventory.removeWithoutSaving(worldConfig.worldname, false);
        }
        worldConfig.inventory = this;
        this.worlds.add(worldConfig.worldname.toLowerCase());
        if (this.worldname == null) {
            this.worldname = getSharedWorldName(this.worlds);
        }
        return true;
    }

    /**
     * Matches world names using a regular expression. Supports a simplified
     * regex GLOB pattern as well.
     */
    public static abstract class MatchRule implements Comparable<MatchRule> {
        private final String expression;

        public final String getExpression() {
            return expression;
        }

        public static MatchRule of(String expression) {
            if (!expression.startsWith("^") && !expression.endsWith("$")) {
                // Try to parse a simplified pattern where # changes into [0-9]+ and * changes into .*
                String patternified = expression
                        .replaceAll("\\.", "\\\\.")
                        .replaceAll("\\*", ".*")
                        .replaceAll("#", "[0-9]+");
                try {
                    final Pattern pattern = Pattern.compile(patternified);
                    return new MatchRule(expression) {
                        @Override
                        public boolean matches(String worldName) {
                            return pattern.matcher(worldName).matches();
                        }
                    };
                } catch (PatternSyntaxException ex) { /* ignore */ }
            }

            // Try to parse as regex pattern
            try {
                final Pattern pattern = Pattern.compile(expression);
                return new MatchRule(expression) {
                    @Override
                    public boolean matches(String worldName) {
                        return pattern.matcher(worldName).matches();
                    }
                };
            } catch (PatternSyntaxException ex) { /* ignore */ }

            // Match exactly as fallback
            return new MatchRule(expression) {
                @Override
                public boolean matches(String worldName) {
                     return worldName.equals(this.getExpression());
                }
            };
        }

        private MatchRule(String expression) {
            this.expression = expression;
        }

        public abstract boolean matches(String worldName);

        @Override
        public int compareTo(MatchRule other) {
            return this.expression.compareTo(other.expression);
        }

        @Override
        public String toString() {
            return "Inventory.MatchRule{" + expression + "}";
        }

        @Override
        public int hashCode() {
            return this.expression.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof MatchRule) {
                return this.expression.equals(((MatchRule) obj).expression);
            } else {
                return false;
            }
        }
    }
}
