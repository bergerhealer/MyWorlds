package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

import com.bergerkiller.bukkit.common.Task;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.internal.CommonBootstrap;
import com.bergerkiller.bukkit.common.nbt.CommonTagCompound;
import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.common.utils.StreamUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.mw.external.MultiverseHandler;
import com.bergerkiller.bukkit.mw.playerdata.PlayerDataFile;
import com.bergerkiller.bukkit.mw.portal.PortalDestination;
import com.bergerkiller.bukkit.mw.portal.PortalMode;
import com.bergerkiller.bukkit.mw.utils.GeneratorStructuresParser;

public class WorldConfig extends WorldConfigStore {
    public final String worldname;
    public String alias;
    public boolean keepSpawnInMemory = true;
    public WorldMode worldmode = WorldMode.NORMAL;
    public boolean loadOnStartup = true;
    private String chunkGeneratorName;
    public Difficulty difficulty = Difficulty.NORMAL;
    private Position spawnPoint; // If null, uses the World spawn
    public RespawnPoint respawnPoint = RespawnPoint.DEFAULT;
    public GameMode gameMode = null;
    public boolean pvp = true;
    public final SpawnControl spawnControl = new SpawnControl();
    public final TimeControl timeControl = new TimeControl(this);
    private PortalDestination defaultNetherPortal = new PortalDestination();
    private PortalDestination defaultEndPortal = new PortalDestination();
    public List<String> rejoinGroup = Collections.emptyList();
    public List<String> OPlist = new ArrayList<String>();
    public boolean allowHunger = true;
    public boolean autosave = true;
    public boolean reloadWhenEmpty = false;
    public boolean formSnow = true;
    public boolean formIce = true;
    public boolean clearInventory = false;
    public boolean rememberLastPlayerPosition = false;
    private BedRespawnMode bedRespawnMode = BedRespawnMode.ENABLED;
    private boolean advancementsEnabled = true;
    public boolean advancementsSilent = false;
    public int playerLimit = -1;
    public WorldInventory inventory;
    private File worldPlayerDataFolderOverride = null;

    protected WorldConfig(String worldname) {
        this.worldname = worldname;
        this.alias = worldname;
    }

    /**
     * Resets all settings in this World Configuration to the defaults.
     * This method acts as if the world is loaded again for the first time.
     */
    public void reset() {
        // Try to import from other World Management plugins
        if (!MyWorlds.importFromMultiVerse || !MultiverseHandler.readWorldConfiguration(this)) {
            // Not imported, (try to) load from the default world configuration
            ConfigurationNode defaultsConfig = getDefaultProperties();
            if (defaultsConfig != null) {
                // Load using a clone to prevent altering the original
                this.load(defaultsConfig);
                if (defaultsConfig.contains(this.worldmode.getName())) {
                    this.load(defaultsConfig.getNode(this.worldmode.getName()));
                }
            }
        }

        // Refresh
        World world = getWorld();
        if (world != null) {
            updateAll(world);
        }
    }

    /**
     * Loads the default settings for a world.
     * This method expects the world to be registered in the mapping prior.
     *
     * @param assignToMatchedInventory Whether to assign this world to an inventory matched by world name
     *                                 This should not happen when loading world configurations on startup
     */
    protected void loadDefaults(boolean assignToMatchedInventory) {
        World world = this.getWorld();
        if (world != null) {
            // Read from the loaded world directly
            this.alias = worldname;
            this.keepSpawnInMemory = world.getKeepSpawnInMemory();
            this.worldmode = WorldMode.get(world);
            this.difficulty = world.getDifficulty();
            this.spawnPoint = null; // Use the world spawn once it becomes available
            this.respawnPoint = RespawnPoint.DEFAULT;
            this.pvp = world.getPVP();
            this.autosave = world.isAutoSave();
            this.getChunkGeneratorName();
        } else {
            this.alias = worldname;
            this.worldmode = WorldMode.get(worldname);
            this.spawnPoint = null; // Use the world spawn once it becomes available
            this.respawnPoint = RespawnPoint.DEFAULT;
            if (WorldManager.worldExists(this.worldname)) {
                // Figure out the world mode by inspecting the region files in the world
                // On failure, it will resort to using the world name to figure it out
                File regions = this.getRegionFolder();
                if (regions != null && regions.isDirectory()) {
                    // Skip the 'region' folder found in the dimension folder
                    regions = regions.getParentFile();
                    // Find out what name the current folder is
                    // If this is DIM1 or DIM-1 then it is the_end/nether
                    // Otherwise we will resort to using the world name
                    String dimName = regions.getName();
                    if (dimName.equals("DIM1")) {
                        this.worldmode = WorldMode.THE_END;
                    } else if (dimName.equals("DIM-1")) {
                        this.worldmode = WorldMode.NETHER;
                    }
                }
            }
        }
        if (MyWorlds.useWorldOperators) {
            for (OfflinePlayer op : Bukkit.getServer().getOperators()) {
                this.OPlist.add(op.getName());
            }
        }

        if (assignToMatchedInventory) {
            this.inventory = WorldInventory.matchOrCreate(this.worldname);
        } else {
            this.inventory = WorldInventory.create(this.worldname);
        }
    }

    /**
     * If this is a mythic dungeons instance, updates inventory sharing rules accordingly
     * so that it shares inventory with the edit session.
     */
    protected void detectMythicDungeonsInstance() {
        // If this is a mythic dungeons world, use the inventory of the edit session instance
        // If this is the edit session then the api returns null.
        World w = getWorld();
        if (w != null) {
            // Find the other dungeon world with the most shared inventories in common
            // If found, merge this world's inventory with it
            MyWorlds.plugin.getMythicDungeonsHelper().getSameDungeonWorlds(w).stream()
                    .map(WorldConfig::get)
                    .map(wc -> wc.inventory)
                    .distinct()
                    .max(Comparator.comparing(inv -> inv.getWorlds().size()))
                    .ifPresent(inventory -> inventory.add(this.worldname));
        }
    }

    /**
     * Detects the generator plugin that is used and whether automatic loading of the world should
     * be disabled because of it (Iris generator bugfix)
     */
    protected void detectGeneratorDisableAutoLoad() {
        String generatorPluginName = WorldManager.getGeneratorPluginName(this.getChunkGeneratorName());

        // Some chunk generator plugins do not handle it when MyWorlds loads the world before
        if (generatorPluginName != null) {
            if (generatorPluginName.equalsIgnoreCase("iris")) {
                this.loadOnStartup = false;
                MyWorlds.plugin.getLogger().log(Level.INFO, "Set auto-load for world '" + worldname +
                        "' to 'no' because it uses chunk generator plugin '" + generatorPluginName + "'!");
            }
        }
    }

    /**
     * Sets the generator name and arguments for this World.
     * Note that this does not alter the generator for a possible loaded world.
     * Only after re-loading does this take effect.
     * 
     * @param name to set to
     */
    public void setChunkGeneratorName(String name) {
        this.chunkGeneratorName = name;
    }

    /**
     * Gets the generator name and arguments of this World
     * 
     * @return Chunk Generator name and arguments
     */
    public String getChunkGeneratorName() {
        if (this.chunkGeneratorName == null) {
            World world = this.getWorld();
            if (world != null) {
                ChunkGenerator gen = world.getGenerator();
                if (gen != null) {
                    Plugin genPlugin = CommonUtil.getPluginByClass(gen.getClass());
                    if (genPlugin != null) {
                        this.chunkGeneratorName = genPlugin.getName();
                    }
                }
            }
        }
        return this.chunkGeneratorName;
    }

    /**
     * Loads all settings of a world over from another world configuration.
     * This method can be called Async and does not update the settings on any
     * loaded worlds.
     * 
     * @param config to load from
     */
    public void load(WorldConfig config) {
        this.alias = config.alias;
        this.keepSpawnInMemory = config.keepSpawnInMemory;
        this.worldmode = config.worldmode;
        this.chunkGeneratorName = config.chunkGeneratorName;
        this.difficulty = config.difficulty;
        this.loadOnStartup = config.loadOnStartup;

        // Copy spawn point. Swap world name if it referred to the original world
        this.spawnPoint = (config.spawnPoint == null) ? null : config.spawnPoint.clone();
        if (this.spawnPoint != null &&
            this.spawnPoint.getWorldName() != null &&
            this.spawnPoint.getWorldName().equals(config.worldname)
        ) {
            this.spawnPoint.setWorldName(this.worldname);
        }

        // Copy respawn point. Swap world name if it referred to the original world spawn
        this.respawnPoint = config.respawnPoint.adjustAfterCopy(config.worldname, this.worldname);

        this.rejoinGroup = config.rejoinGroup; // Is immutable
        this.gameMode = config.gameMode;
        this.allowHunger = config.allowHunger;
        this.pvp = config.pvp;
        this.spawnControl.deniedCreatures.clear();
        this.spawnControl.deniedCreatures.addAll(config.spawnControl.deniedCreatures);
        this.timeControl.setLocking(config.timeControl.isLocked());
        this.timeControl.setTime(timeControl.getTime());
        this.autosave = config.autosave;
        this.reloadWhenEmpty = config.reloadWhenEmpty;
        this.formSnow = config.formSnow;
        this.formIce = config.formIce;
        this.clearInventory = config.clearInventory;
        this.bedRespawnMode = config.bedRespawnMode;
        this.advancementsEnabled = config.advancementsEnabled;
        this.advancementsSilent = config.advancementsSilent;
        this.playerLimit = config.playerLimit;
        this.inventory = config.inventory.add(this.worldname);
    }

    public void load(ConfigurationNode node) {
        this.alias = node.get("alias", this.worldname);
        this.keepSpawnInMemory = node.get("keepSpawnLoaded", this.keepSpawnInMemory);
        this.worldmode = WorldMode.get(node.get("environment", this.worldmode.getName()));
        this.chunkGeneratorName = node.get("chunkGenerator", String.class, this.chunkGeneratorName);
        this.loadOnStartup = !node.contains("loaded") || !"ignore".equalsIgnoreCase(node.get("loaded", String.class, ""));
        if (LogicUtil.nullOrEmpty(this.chunkGeneratorName)) {
            this.chunkGeneratorName = null;
        }
        this.difficulty = node.get("difficulty", Difficulty.class, this.difficulty);
        this.gameMode = node.get("gamemode", GameMode.class, this.gameMode);
        this.clearInventory = node.get("clearInventory", this.clearInventory);

        // Respawn point
        if (node.isNode("respawn")) {
            this.respawnPoint = RespawnPoint.fromConfig(node.getNode("respawn"));
        } else {
            this.respawnPoint = RespawnPoint.DEFAULT;
        }

        // Spawn point of the World. If set to another world, ignore,
        // and use the Bukkit world spawn info for this instead if available.
        String worldspawn = node.get("spawn.world", String.class);
        if (worldspawn != null) {
            double x = node.get("spawn.x", 0.0);
            double y = node.get("spawn.y", 128.0);
            double z = node.get("spawn.z", 0.0);
            float yaw = node.get("spawn.yaw", 0.0f);
            float pitch = node.get("spawn.pitch", 0.0f);
            if (worldspawn.equals(this.worldname)) {
                this.spawnPoint = new Position(worldspawn, x, y, z, yaw, pitch);
            } else {
                // Mutate respawn point, instead
                this.respawnPoint = new RespawnPoint.RespawnPointLocation(worldspawn, x, yaw, z, yaw, pitch);
                // Set spawn point to whatever the World one is set to
                this.spawnPoint = null;
            }
        }

        // List of world names included when using the /world rejoin command
        if (node.contains("rejoinGroup")) {
            this.rejoinGroup = Collections.unmodifiableList(new ArrayList<>(
                    node.getList("rejoinGroup", String.class)));
        } else {
            this.rejoinGroup = Collections.emptyList();
        }

        this.formIce = node.get("formIce", this.formIce);
        this.formSnow = node.get("formSnow", this.formSnow);
        this.pvp = node.get("pvp", this.pvp);
        this.allowHunger = node.get("hunger", this.allowHunger);
        this.rememberLastPlayerPosition = node.get("rememberlastplayerpos", this.rememberLastPlayerPosition);
        this.reloadWhenEmpty = node.get("reloadWhenEmpty", this.reloadWhenEmpty);

        if (node.contains("bedRespawnEnabled")) {
            this.bedRespawnMode = node.get("bedRespawnEnabled", this.bedRespawnMode != BedRespawnMode.DISABLED)
                    ? BedRespawnMode.ENABLED : BedRespawnMode.DISABLED;
        } else {
            this.bedRespawnMode = node.get("bedRespawnMode", this.bedRespawnMode);
        }

        this.advancementsEnabled = node.get("advancementsEnabled", this.advancementsEnabled);
        this.advancementsSilent = node.get("advancementsSilent", this.advancementsSilent);
        this.playerLimit = node.get("playerLimit", this.playerLimit);
        for (String type : node.getList("deniedCreatures", String.class)) {
            type = type.toUpperCase();
            if (type.equals("ANIMALS")) {
                this.spawnControl.setAnimals(true);
            } else if (type.equals("MONSTERS")) {
                this.spawnControl.setMonsters(true);
            } else {
                EntityType t = ParseUtil.parseEnum(EntityType.class, type, null);
                if (t != null) {
                    this.spawnControl.deniedCreatures.add(t);
                }
            }
        }
        long time = (long) node.get("lockedtime", Integer.MIN_VALUE);
        if (time != Integer.MIN_VALUE) {
            this.timeControl.setTime(time);
            this.timeControl.setLocking(true);
        }

        // Compatibility with very old configuration formats
        if (node.contains("defaultPortal")) {
            node.set("defaultNetherPortal", node.get("defaultPortal"));
            node.remove("defaultPortal");
        }

        this.defaultNetherPortal = PortalDestination.fromConfig(node, "defaultNetherPortal");
        this.defaultEndPortal = PortalDestination.fromConfig(node, "defaultEndPortal");

        this.OPlist = node.getList("operators", String.class, this.OPlist);

        // Overrides the player data folder. Used when saving player data when inventories are split.
        {
            String playerDataFolderName = node.get("playerDataFolder", "");
            if (playerDataFolderName.isEmpty()) {
                this.worldPlayerDataFolderOverride = null;
            } else {
                this.worldPlayerDataFolderOverride = new File(playerDataFolderName);
            }
        }
    }

    public void saveDefault(ConfigurationNode node) {
        save(node);
        // Remove nodes we rather not see
        node.remove("environment");
        node.remove("name");
        node.remove("alias");
        node.remove("chunkGenerator");
        node.remove("spawn");
        node.remove("loaded");
        node.remove("rejoinGroup");
        node.remove("defaultNetherPortal");
        node.remove("defaultEndPortal");
        node.remove("playerDataFolder");
    }

    public void save(ConfigurationNode node) {
        //Set if the world can be directly accessed
        World w = this.getWorld();
        if (w != null) {
            this.difficulty = w.getDifficulty();
            this.keepSpawnInMemory = w.getKeepSpawnInMemory();
            this.autosave = w.isAutoSave();
        }
        if (this.worldname == null || this.worldname.equals(this.getConfigName())) {
            node.remove("name");
        } else {
            node.set("name", this.worldname);
        }
        if (this.alias == null || this.alias.equals(this.worldname)) {
            node.remove("alias");
        } else {
            node.set("alias", this.alias);
        }
        if (this.loadOnStartup) {
            node.set("loaded", w != null);
        } else {
            node.set("loaded", "ignore");
        }
        node.set("keepSpawnLoaded", this.keepSpawnInMemory);
        node.set("environment", this.worldmode.getName());
        node.set("chunkGenerator", LogicUtil.fixNull(this.getChunkGeneratorName(), ""));
        node.set("clearInventory", this.clearInventory ? true : null);
        node.set("gamemode", this.gameMode == null ? "NONE" : this.gameMode.toString());

        if (this.timeControl.isLocked()) {
            node.set("lockedtime", this.timeControl.getTime());
        } else {
            node.remove("lockedtime");
        }

        ArrayList<String> creatures = new ArrayList<String>();
        for (EntityType type : this.spawnControl.deniedCreatures) {
            creatures.add(type.name());
        }
        node.set("rememberlastplayerpos", this.rememberLastPlayerPosition);
        node.set("pvp", this.pvp);
        PortalDestination.toConfig(this.defaultNetherPortal, node, "defaultNetherPortal");
        PortalDestination.toConfig(this.defaultEndPortal, node, "defaultEndPortal");
        node.set("operators", this.OPlist);
        node.set("deniedCreatures", creatures);
        node.set("hunger", this.allowHunger);
        node.set("formIce", this.formIce);
        node.set("formSnow", this.formSnow);
        node.set("difficulty", this.difficulty == null ? "NONE" : this.difficulty.toString());
        node.set("reloadWhenEmpty", this.reloadWhenEmpty);
        node.set("bedRespawnMode", this.bedRespawnMode);
        node.remove("bedRespawnEnabled");
        node.set("advancementsEnabled", this.advancementsEnabled);
        node.set("advancementsSilent", this.advancementsSilent);
        node.set("playerLimit", this.playerLimit);
        node.set("playerDataFolder", (worldPlayerDataFolderOverride == null)
                ? "" : worldPlayerDataFolderOverride.toString());

        if (this.spawnPoint == null) {
            node.remove("spawn");
        } else {
            node.set("spawn.world", this.spawnPoint.getWorldName());
            node.set("spawn.x", this.spawnPoint.getX());
            node.set("spawn.y", this.spawnPoint.getY());
            node.set("spawn.z", this.spawnPoint.getZ());
            node.set("spawn.yaw", (double) this.spawnPoint.getYaw());
            node.set("spawn.pitch", (double) this.spawnPoint.getPitch());
        }

        if (this.respawnPoint == RespawnPoint.DEFAULT) {
            node.remove("respawn");
        } else {
            node.set("respawn", this.respawnPoint.toConfig());
        }

        if (rejoinGroup.isEmpty()) {
            node.remove("rejoinGroup");
        } else {
            node.set("rejoinGroup", rejoinGroup);
        }
    }

    /**
     * Tries to find the spawn point of this World. If the world isn't loaded,
     * it tries to read the level.dat to find it.
     *
     * @return World spawn position
     */
    public Position tryFindSpawnPositionOffline() {
        // If set, return it verbatim
        if (this.spawnPoint != null) {
            return this.spawnPoint.clone();
        }

        // If loaded, try the World spawn
        World world = this.getWorld();
        if (world != null) {
            return new Position(world.getSpawnLocation());
        }

        // Try to read the level.dat file to find it
        CommonTagCompound data = getData();
        if (data != null) {
            double[] pos = data.getValue("Pos", double[].class);
            float[] rot = data.getValue("Rotation", float[].class);
            if (pos != null) {
                if (rot == null) {
                    rot = new float[] { 0.0f, 0.0f };
                }
                return new Position(this.worldname, pos[0], pos[1], pos[2], rot[0], rot[1]);
            }
        }

        // Absolutely NO idea, return a generic position
        return new Position(this.worldname, 0, 128, 0);
    }

    public boolean isAdvancementsEnabled() {
        return advancementsEnabled;
    }

    public void setAdvancementsEnabled(boolean enabled) {
        if (advancementsEnabled != enabled) {
            advancementsEnabled = enabled;
            if (!enabled) {
                MyWorlds.plugin.getAdvancementManager().notifyAdvancementsDisabledOnWorld();
            }
        }
    }

    public BedRespawnMode getBedRespawnMode() {
        return bedRespawnMode;
    }

    public void setBedRespawnMode(BedRespawnMode bedRespawnMode) {
        this.bedRespawnMode = bedRespawnMode;
        if (!bedRespawnMode.persistInProfile()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                WorldManager.removeInvalidBedSpawnPoint(player);
            }
        }
    }

    /**
     * Gets the Spawn location set for this world currently. If none is configured yet,
     * returns what is set by Bukkit.
     *
     * @return Spawn location, null if this World isn't loaded
     */
    public Location getSpawnLocation() {
        // If set, turn it into a Location
        if (this.spawnPoint != null) {
            return this.spawnPoint.toLocation();
        }

        World world = this.getWorld();
        return (world == null) ? null : world.getSpawnLocation();
    }

    /**
     * Sets a new spawn point for this World. This is where players are teleported to
     * when teleporting to this World.
     *
     * @param location
     */
    public void setSpawnLocation(Location location) {
        if (location != null && location.getWorld() != this.getWorld()) {
            throw new IllegalArgumentException("Can not set the spawn point to another world");
        }

        this.spawnPoint = (location == null) ? null : new Position(location);
        if (location != null) {
            setBukkitSpawn(location.getWorld(), location);
        }
    }

    /**
     * Sets a new spawn point for this World. This is where players are teleported to
     * when teleporting to this World.
     *
     * @param location
     */
    public void setSpawnLocation(Position location) {
        if (location != null && location.getWorldName().equalsIgnoreCase(this.worldname)) {
            throw new IllegalArgumentException("Can not set the spawn point to another world");
        }
        this.spawnPoint = location.clone();
        if (location != null) {
            World world = this.getWorld();
            if (world != null) {
                setBukkitSpawn(world, location.toLocation(world));
            }
        }
    }

    /**
     * Regenerates the spawn point for a world if it is not properly set<br>
     * Also updates the spawn position in the world configuration
     */
    public void fixSpawnLocation() {
        fixSpawnLocation(this.getWorld());
    }

    /**
     * Regenerates the spawn point for a world if it is not properly set<br>
     * Also updates the spawn position in the world configuration
     * 
     * @param world of the spawn point (in case world isn't accessible by name yet)
     */
    public void fixSpawnLocation(World world) {
        if (world == null) {
            return; // Can't do anything yet
        }

        // Get current spawn point location.
        Location spawnLocation;
        if (this.spawnPoint != null) {
            spawnLocation = this.spawnPoint.toLocation(world);
        } else {
            spawnLocation = world.getSpawnLocation();
        }

        // Compute new spawn location
        Location fixedSpawnLocation = WorldManager.getSafeSpawn(spawnLocation);

        // Apply to the World and our configuration (if it was set)
        if (fixedSpawnLocation != null) { // Just in case
            setBukkitSpawn(world, fixedSpawnLocation);
            if (this.spawnPoint != null) {
                this.spawnPoint = new Position(fixedSpawnLocation);
            }
        }
    }

    /**
     * Gets the default nether portal destination for nether portals on this World.
     *
     * @return Default nether portal destination
     */
    public PortalDestination getDefaultNetherPortalDestination() {
        return this.defaultNetherPortal;
    }

    public void setDefaultNetherPortalDestination(PortalDestination destination) {
        if (destination == null) {
            throw new IllegalArgumentException("Can not be null");
        }
        this.defaultNetherPortal = destination;
    }

    /**
     * Gets the default end portal destination for end portals on this World.
     *
     * @return Default end portal destination
     */
    public PortalDestination getDefaultEndPortalDestination() {
        return this.defaultEndPortal;
    }

    public void setDefaultEndPortalDestination(PortalDestination destination) {
        if (destination == null) {
            throw new IllegalArgumentException("Can not be null");
        }
        this.defaultEndPortal = destination;
    }

    public PortalDestination getDefaultDestination(PortalType portalType) {
        switch (portalType) {
        case NETHER:
            return getDefaultNetherPortalDestination();
        case END:
            return getDefaultEndPortalDestination();
        default:
            return new PortalDestination();
        }
    }

    public void setDefaultDestination(PortalType portalType, PortalDestination destination) {
        switch (portalType) {
        case NETHER:
            setDefaultNetherPortalDestination(destination);
            break;
        case END:
            setDefaultEndPortalDestination(destination);
            break;
        default:
            break;
        }
    }

    /**
     * Fired right before a player respawns.
     * Note that no leave is fired after this event.
     * It is a replacement for leaving a world (if that is the case)
     * 
     * @param player that respawns
     */
    public void onRespawn(Player player, Location respawnLocation) {
        MyWorlds.plugin.getPlayerDataController().onRespawnSave(player, respawnLocation);
    }

    /**
     * Fired when a player leaves this world
     * 
     * @param player that is about to leave this world
     * @param quit state: True if the player left the server, False if not
     */
    public void onPlayerLeave(Player player, boolean quit) {
        // If not quiting (it saves then anyhow!) save the old information
        if (!quit) {
            MWPlayerDataController.savePlayer(player, this.getWorld());
        }
    }

    /**
     * Fired when a player left this world and already joined another world.
     * If the old player position, inventory and etc. is important, add the logic in
     * {@link #onPlayerLeave(Player, boolean)} instead.
     * 
     * @param player that left this world (may no longer contain valid information)
     */
    public void onPlayerLeft(Player player) {
        this.updateReload();
    }

    /**
     * Fired when a player joined this world
     * 
     * @param player that just entered this world
     * @param isJoin Whether this is a PlayerJoin event
     */
    public void onPlayerEnter(Player player, boolean isJoin) {
        // Refresh states based on the new world the player joined
        if (!isJoin) {
            MyWorlds.plugin.getPlayerDataController().refreshState(player);
        }

        // Apply world-specific settings
        updateOP(player);
        updateGamemode(player);
        updateHunger(player);
        WorldManager.removeInvalidBedSpawnPoint(player);
        // Store advancements
        MyWorlds.plugin.getAdvancementManager().cacheAdvancements(player);
    }

    public void onWorldLoad(World world) {
        // Update settings
        updateAll(world);
        // Detect default portals
        tryCreatePortalLink();
        // Link inventories if it is a mythic dungeons instance
        // Also run this next-tick, just in case the instance isn't initialized yet during world load.
        detectMythicDungeonsInstance();
        CommonUtil.nextTick(() -> detectMythicDungeonsInstance());
        // If advancements are disabled on this world, let the advancement manager know
        if (!advancementsEnabled) {
            MyWorlds.plugin.getAdvancementManager().notifyAdvancementsDisabledOnWorld();
        }
    }

    public void onWorldUnload(World world) {
        // If the actual World spawnpoint changed, be sure to update accordingly
        // This is so that if another plugin changes the spawn point, MyWorlds updates too
        if (this.spawnPoint != null) {
            Location spawn = world.getSpawnLocation();
            if (spawnPoint.getBlockX() != spawn.getBlockX() || spawnPoint.getBlockY() != spawn.getBlockY() 
                    || spawnPoint.getBlockZ() != spawn.getBlockZ()) {
                spawnPoint = new Position(spawn);
            }
        }

        // Disable time control
        timeControl.updateWorld(null);

        // Purge from cache
        purgeWorldFromByWorldLookup(world);
    }

    public World loadWorld() {
        if (WorldManager.worldExists(this.worldname)) {
            World w = WorldManager.getOrCreateWorld(this.worldname);
            if (w == null) {
                MyWorlds.plugin.log(Level.SEVERE, "Failed to (pre)load world: " + worldname);
            } else {
                return w;
            }
        } else {
            MyWorlds.plugin.log(Level.WARNING, "World: " + worldname + " could not be loaded because it no longer exists!");
        }
        return null;
    }
    public boolean unloadWorld() {
        return WorldManager.unload(this.getWorld());
    }

    /**
     * Updates all components of this World Configuration to the loaded world specified
     * 
     * @param world to apply it to
     */
    public void updateAll(World world) {
        // Fix spawn point if needed
        // Do this one tick delayed as it might mess with other plugins otherwise
        (new FindSafeSpawnTask()).start(1);

        // Apply configured spawn point to world
        if (this.spawnPoint != null) {
            setBukkitSpawn(world, this.spawnPoint.toLocation(world));
        }

        // Update world settings
        updatePVP(world);
        updateKeepSpawnInMemory(world);
        updateDifficulty(world);
        updateAutoSave(world);
        updateAdvancements(world);
        timeControl.updateWorld(world);
    }
    public void updateReload() {
        World world = this.getWorld();
        if (world == null) return;
        if (!this.reloadWhenEmpty) return;
        if (world.getPlayers().size() > 0) return;
        //reload world
        MyWorlds.plugin.log(Level.INFO, "Reloading world '" + worldname + "' - world became empty");
        if (!this.unloadWorld()) {
            MyWorlds.plugin.log(Level.WARNING, "Failed to unload world: " + worldname + " for reload purposes");
        } else if (this.loadWorld() == null) {
            MyWorlds.plugin.log(Level.WARNING, "Failed to load world: " + worldname + " for reload purposes");
        } else {
            MyWorlds.plugin.log(Level.INFO, "World reloaded successfully");
        }
    }
    public void updateAutoSave(World world) {
        if (world != null && world.isAutoSave() != this.autosave) {
            world.setAutoSave(this.autosave);
        }
    }
    public void updateOP(Player player) {
        if (MyWorlds.useWorldOperators) {
            boolean op = this.isOP(player);
            if (op != player.isOp()) {
                player.setOp(op);
                if (op) {
                    player.sendMessage(ChatColor.YELLOW + "You are now op!");
                } else {
                    player.sendMessage(ChatColor.RED + "You are no longer op!");
                }
            }
        }
    }
    public void updateOP(World world) {
        if (MyWorlds.useWorldOperators) {
            for (Player p : world.getPlayers()) updateOP(p);
        }
    }
    public void updateHunger(Player player) {
        if (!allowHunger) {
            player.setFoodLevel(20);
        }
    }
    public void updateHunger(World world) {
        for (Player player : WorldUtil.getPlayers(world)) {
            updateHunger(player);
        }
    }
    public void updateGamemode(Player player) {
        if (this.gameMode != null && (MyWorlds.forceGamemodeChanges || !Permission.GENERAL_IGNOREGM.has(player))) {
            player.setGameMode(this.gameMode);
        }
    }
    public void updatePVP(World world) {
        if (world != null && this.pvp != world.getPVP()) {
            world.setPVP(this.pvp);
        }
    }
    public void updateKeepSpawnInMemory(World world) {
        if (world != null && world.getKeepSpawnInMemory() != this.keepSpawnInMemory) {
            world.setKeepSpawnInMemory(this.keepSpawnInMemory);
        }
    }
    public void updateDifficulty(World world) {
        if (world != null && world.getDifficulty() != this.difficulty) {
            world.setDifficulty(this.difficulty);
        }
    }
    public void updateAdvancements(World world) {
        try {
            MyWorlds.plugin.getAdvancementManager().applyGameRule(world, this.advancementsEnabled && !this.advancementsSilent);
        } catch (Throwable t) {
            MyWorlds.plugin.getLogger().log(Level.WARNING, "Failed to update advancements enabled setting for world " + world.getName(), t);
        }
    }

    /**
     * Checks whether the player can teleport/join a particular world according to the per-world
     * player limits and the bypass permission. If the player cannot join it, message that
     * the world is full.
     *
     * @param player
     * @return True if the player is allowed to join the world
     */
    public boolean checkPlayerLimit(Player player) {
        if (playerLimit <= -1) {
            return true;
        } else if (Permission.GENERAL_BYPASSPLAYERLIMITS.has(player)) {
            return true;
        } else {
            return getNumberOfPlayersLimited() < playerLimit;
        }
    }

    public int getNumberOfPlayersLimited() {
        World world = this.getWorld();
        return (world == null) ? 0 : world.getPlayers().size();
    }

    public boolean isTeleportingToLastPosition(Player player) {
        if (this.rememberLastPlayerPosition) {
            return true;
        }
        if (MyWorlds.keepLastPositionPermissionEnabled && Permission.GENERAL_KEEPLASTPOS.has(player)) {
            return true;
        }
        return false;
    }

    /**
     * Gets a safe configuration name for this World Configuration<br>
     * Unsafe characters, such as dots, are replaced
     * 
     * @return Safe config world name
     */
    public String getConfigName() {
        if (this.worldname == null) {
            return "";
        }
        return this.worldname.replace('.', '_').replace(':', '_');
    }

    /**
     * Gets the loaded World of this world configuration<br>
     * If the world is not loaded, null is returned
     * 
     * @return the World
     */
    public World getWorld() {
        return this.worldname == null ? null : WorldManager.getWorld(this.worldname);
    }

    public boolean isOP(Player player) {
        for (String playername : OPlist) {
            if (playername.equals("\\*")) return true;
            if (player.getName().equalsIgnoreCase(playername)) return true;
        }
        return false;
    }
    public void setGameMode(GameMode mode) {
        if (this.gameMode != mode) {
            this.gameMode = mode;
            if (mode != null) {
                World world = this.getWorld();
                if (world != null) {
                    for (Player p : world.getPlayers()) {
                        this.updateGamemode(p);
                    }
                }
            }
        }
    }

    /**
     * Tries to create a portal link with another world currently available.
     * 
     * @see #tryCreatePortalLink(WorldConfig, WorldConfig)
     */
    public void tryCreatePortalLink() {
        // Try to detect a possible portal link between worlds
        for (WorldConfig otherConfig : WorldConfig.all()) {
            if (otherConfig != this) {
                WorldConfig.tryCreatePortalLink(this, otherConfig);
            }
        }
    }

    /**
     * Tries to create a default portal link if none is currently set between two worlds
     * Only if the worlds match names and no portal is set will this create a link.
     * 
     * @param world1 First configuration of the world of the portal link
     * @param world2 Second configuration of the world of the portal link
     */
    public static void tryCreatePortalLink(WorldConfig world1, WorldConfig world2) {
        // Name compatibility check
        if (!world1.getRawWorldName().equals(world2.getRawWorldName())) {
            return;
        }
        // If same environment, directly stop it here
        if (world1.worldmode == world2.worldmode) {
            return;
        }

        // Make sure world1 always stores the NORMAL mode
        if (world2.worldmode == WorldMode.NORMAL) {
            WorldConfig tmp = world1;
            world1 = world2;
            world2 = tmp;
        }
        if (world1.worldmode != WorldMode.NORMAL) {
            return;
        }

        // Default nether portal check and detection
        if (world2.worldmode.getEnvironment() == World.Environment.NETHER) {
            if (world1.defaultNetherPortal.canAutoDetect()) {
                world1.defaultNetherPortal = new PortalDestination();
                world1.defaultNetherPortal.setMode(PortalMode.NETHER_LINK);
                world1.defaultNetherPortal.setName(world2.worldname);
                world1.defaultNetherPortal.setPlayersOnly(false);
                world1.defaultNetherPortal.setAutoDetectEnabled(true);
                MyWorlds.plugin.log(Level.INFO, "Created nether portal link from world '" + world1.worldname + "' to '" + world2.worldname + "'!");
            }
            if (world2.defaultNetherPortal.canAutoDetect()) {
                world2.defaultNetherPortal = new PortalDestination();
                world2.defaultNetherPortal.setMode(PortalMode.NETHER_LINK);
                world2.defaultNetherPortal.setName(world1.worldname);
                world2.defaultNetherPortal.setPlayersOnly(false);
                world2.defaultNetherPortal.setAutoDetectEnabled(true);
                MyWorlds.plugin.log(Level.INFO, "Created nether portal link from world '" + world2.worldname + "' to '" + world1.worldname + "'!");
            }
        }

        // Default ender portal check and detection
        if (world2.worldmode.getEnvironment() == World.Environment.THE_END) {
            // World 1 is the normal world, and it teleports to the end on a platform
            // World 2 is the end world, and it shows the end credits when teleported
            if (world1.defaultEndPortal.canAutoDetect()) {
                world1.defaultEndPortal = new PortalDestination();
                world1.defaultEndPortal.setMode(PortalMode.END_PLATFORM);
                world1.defaultEndPortal.setName(world2.worldname);
                world1.defaultEndPortal.setPlayersOnly(false);
                world1.defaultEndPortal.setShowCredits(false);
                world1.defaultEndPortal.setAutoDetectEnabled(true);
                MyWorlds.plugin.log(Level.INFO, "Created end portal gateway link from world '" + world1.worldname + "' to '" + world2.worldname + "'!");
            }
            if (world2.defaultEndPortal.canAutoDetect()) {
                world2.defaultEndPortal = new PortalDestination();
                world2.defaultEndPortal.setMode(PortalMode.RESPAWN);
                world2.defaultEndPortal.setName(world1.worldname);
                world2.defaultEndPortal.setPlayersOnly(true);
                world2.defaultEndPortal.setShowCredits(true);
                world2.defaultEndPortal.setAutoDetectEnabled(true);
                MyWorlds.plugin.log(Level.INFO, "Created end portal (show credits) link from world '" + world2.worldname + "' to '" + world1.worldname + "'!");
            }
        }

        // By default respawn on the overworld
        if (world2.respawnPoint == RespawnPoint.DEFAULT) {
            world2.respawnPoint = new RespawnPoint.RespawnPointWorldSpawn(world1.worldname);
            MyWorlds.plugin.log(Level.INFO, "Players that die in world '" + world2.worldname + "' will respawn in world '" + world1.worldname + "'!");
        }
    }

    /**
     * Gets the World Name of this World excluding any _nether, _the_end or DIM extensions from the name.
     * Returns an empty String if the world name equals DIM1 or DIM-1 (for MCPC+ server).
     * In all cases, a lower-cased world name is returned.
     * 
     * @return Raw world name
     */
    private String getRawWorldName() {
        String lower = this.worldname.toLowerCase();
        if (lower.endsWith("_nether")) {
            return lower.substring(0, lower.length() - 7);
        }
        if (lower.endsWith("_the_end")) {
            return lower.substring(0, lower.length() - 8);
        }
        if (lower.equals("dim1") || lower.equals("dim-1")) {
            return "world";
        }
        return lower;
    }

    /**
     * Gets the File folder where the data of this World is stored
     * 
     * @return World Folder
     */
    public File getWorldFolder() {
        return WorldUtil.getWorldFolder(this.worldname);
    }

    /**
     * Gets the File folder where player data of this World is saved
     * 
     * @return Player data folder
     */
    public File getPlayerFolder() {
        if (worldPlayerDataFolderOverride != null) {
            return worldPlayerDataFolderOverride;
        }

        World world = getWorld();
        if (world == null) {
            return new File(getWorldFolder(), "playerdata");
        } else {
            return WorldUtil.getPlayersFolder(world);
        }
    }

    /**
     * Gets the File where player data for this world is saved
     *
     * @param playerUUID UUID String of the Player to get this world's save file for.
     *                   Also works for invalid uuid names to locate files by that name.
     * @return Player Data File
     */
    public File getPlayerData(String playerUUID) {
        return PlayerDataFile.getPlayerDataFile(getPlayerFolder(), playerUUID);
    }

    /**
     * Gets the File where player data for this world is saved
     * 
     * @param player to get this world's save file for
     * @return Player Data File
     */
    public File getPlayerData(OfflinePlayer player) {
        return PlayerDataFile.getPlayerDataFile(getPlayerFolder(), player.getUniqueId());
    }

    /**
     * Gets the File Location where the regions of this world are contained
     * 
     * @return Region Folder
     */
    public File getRegionFolder() {
        return WorldUtil.getWorldRegionFolder(this.worldname);
    }

    /**
     * Gets the File pointing to the level.dat of the world
     * 
     * @return Data File
     */
    public File getDataFile() {
        if (Common.hasCapability("Common:WorldUtil:getWorldLevelFile")) {
            return getDataFileUsingBKCLAPI();
        } else {
            // Note: not correct for forge servers
            return new File(getWorldFolder(), "level.dat");
        }
    }

    private File getDataFileUsingBKCLAPI() {
        return WorldUtil.getWorldLevelFile(this.worldname);
    }

    /**
     * Gets the File pointing to the uid.dat of the world
     * 
     * @return UID File
     */
    public File getUIDFile() {
        return new File(getWorldFolder(), "uid.dat");
    }

    /**
     * Gets the amount of bytes of data stored on disk about this World
     * 
     * @return World file size
     */
    public long getWorldSize() {
        return Util.getFileSize(getWorldFolder());
    }

    /**
     * Turns the {@link #rejoinGroup} config option into a list of (existing) WorldConfig instances,
     * including this world config itself.
     *
     * @return rejoin world configs
     */
    public List<WorldConfig> getRejoinGroupWorldConfigs() {
        if (rejoinGroup.isEmpty()) {
            return Collections.singletonList(this);
        } else {
            List<WorldConfig> worldConfigs = new ArrayList<>();
            worldConfigs.add(this);
            for (String name : rejoinGroup) {
                WorldConfig wc = WorldConfig.getIfExists(name);
                if (wc != null && wc.isLoaded()) {
                    worldConfigs.add(wc);
                }
            }
            return worldConfigs;
        }
    }

    /**
     * Creates a new Data compound for this World, storing the default values
     * 
     * @param seed to use
     * @return Data compound
     */
    public CommonTagCompound createData(long seed) {
        String args = this.getChunkGeneratorName();
        if (args == null) {
            args = ""; // Eh.
        }

        // Trim generator plugin name
        {
            int idx = args.indexOf(':');
            if (idx != -1) {
                args = args.substring(idx + 1);
            }
        }

        GeneratorStructuresParser structuresOption = new GeneratorStructuresParser();
        String options = structuresOption.process(args);

        CommonTagCompound data = new CommonTagCompound();
        data.putValue("thundering", (byte) 0);
        data.putValue("thundering", (byte) 0);
        data.putValue("LastPlayed", System.currentTimeMillis());

        // We abuse data conversion so we can put old-style options, and theyll automatically be
        // converted to what is appropriate for a modern MC version.
        // This supports the legacy-style options string, instead of just JSON
        // Most of this changes around MC 1.16!
        data.putValue("RandomSeed", seed);
        data.putValue("generatorName", worldmode.getTypeName());
        data.putValue("generatorVersion", 0);
        data.putValue("generatorOptions", options);
        if (structuresOption.hasNoStructures) {
            data.putValue("MapFeatures", false);
        } else if (structuresOption.hasStructures) {
            data.putValue("MapFeatures", true);
        }

        data.putValue("version", 19133);
        data.putValue("initialized", (byte) 0); // Spawn point needs to be re-initialized, etc.
        data.putValue("Time", 0L);
        data.putValue("raining", (byte) 0);
        data.putValue("SpawnX", 0);
        data.putValue("thunderTime", 200000000);
        data.putValue("SpawnY", 64);
        data.putValue("SpawnZ", 0);
        data.putValue("LevelName", worldname);
        data.putValue("SizeOnDisk", getWorldSize());
        data.putValue("rainTime", 50000);
        return data;
    }

    /**
     * Gets the Data compound stored for this World
     * 
     * @return World Data
     */
    public CommonTagCompound getData() {
        File f = getDataFile();
        if (!f.exists()) {
            return null;
        }
        try {
            CommonTagCompound root = CommonTagCompound.readFromFile(f, true);
            if (root != null) {
                return root.get("Data", CommonTagCompound.class);
            } else {
                return null;
            }
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Sets the Data compound stored by this world
     * 
     * @param data to set to
     * @return True if successful, False if not
     */
    public boolean setData(CommonTagCompound data) {
        try {
            CommonTagCompound root = new CommonTagCompound();
            root.put("Data", data);
            FileOutputStream out = StreamUtil.createOutputStream(getDataFile());
            try {
                root.writeToStream(out, true);
            } finally {
                out.close();
            }
            return true;
        } catch (IOException e) {
            MyWorlds.plugin.getLogger().log(Level.SEVERE, "Unhandled error trying to save world level.dat of " + this.worldname, e);
            return false;
        }
    }

    /**
     * Recreates the data file of a world
     * 
     * @param seed to store in the new data file
     * @return True if successful, False if not
     */
    public boolean resetData(String seed) {
        return resetData(WorldManager.getRandomSeed(seed));
    }

    /**
     * Recreates the data file of a world
     * 
     * @param seed to store in the new data file
     * @return True if successful, False if not
     */
    public boolean resetData(long seed) {
        return setData(createData(seed));
    }

    /**
     * Reads the WorldInfo structure from this World
     * 
     * @return WorldInfo structure
     */
    public WorldInfo getInfo() {
        WorldInfo info = null;
        try {
            CommonTagCompound t = getData();
            if (t != null) {
                info = new WorldInfo();
                info.seed = t.getValue("RandomSeed", 0L);
                info.time = t.getValue("Time", 0L);
                info.raining = t.getValue("raining", (byte) 0) != 0;
                info.thundering = t.getValue("thundering", (byte) 0) != 0;
                info.weather_endless = t.getValue("rainTime", 0) > (Integer.MAX_VALUE / 2);
            }
        } catch (Exception ex) {}
        World w = getWorld();
        if (w != null) {
            if (info == null) {
                info = new WorldInfo();
            }
            info.seed = w.getSeed();
            info.time = w.getFullTime();
            info.raining = w.hasStorm();
            info.thundering = w.isThundering();
            info.weather_endless = w.getWeatherDuration() > (Integer.MAX_VALUE / 2);
        }
        if (info != null && MyWorlds.calculateWorldSize) {
            info.size = getWorldSize();
        }
        return info;
    }

    /**
     * Gets whether this World exists on disk at all.
     * If this is not the case, this configuration is either temporary or invalid.
     * Non-existing World Configurations will automatically get purged upon saving.
     * 
     * @return True if existing, False if not
     */
    public boolean isExisting() {
        return WorldManager.worldExists(this.worldname);
    }

    /**
     * Gets whether this World is currently loaded
     * 
     * @return True if loaded, False if not
     */
    public boolean isLoaded() {
        return getWorld() != null;
    }

    /**
     * Gets whether this World is broken (and needs repairs to properly load)
     * 
     * @return True if broken, False if not
     */
    public boolean isBroken() {
        return getData() == null && !isLoaded();
    }

    /**
     * Gets whether the world data of this World has been initialized.
     * If not initialized, then the spawn position needs to be calculated, among other things.
     * 
     * @return True if initialized, False if not
     */
    public boolean isInitialized() {
        CommonTagCompound data = getData();
        return data != null && data.getValue("initialized", true);
    }

    /**
     * Copies this World and all of it's set configuration to a new world under a new name.
     * The World Configuration must be gotten prior as to support Async operations.
     * 
     * @param newWorldConfig for the world to copy to
     * @return True if successful, False if the operation (partially) failed
     */
    public boolean copyTo(WorldConfig newWorldConfig) {
        // If new world name is already occupied - abort
        if (WorldManager.worldExists(newWorldConfig.worldname)) {
            return false;
        }

        // Copy the world folder over
        if (!StreamUtil.tryCopyFile(this.getWorldFolder(), newWorldConfig.getWorldFolder())) {
            return false;
        }

        // Take over the world configuration
        newWorldConfig.load(this);

        // Delete the UID file, as the new world is unique (forces regeneration)
        File uid = newWorldConfig.getUIDFile();
        if (uid.exists()) {
            uid.delete();
        }

        // Update the name set in the level.dat for the new world
        CommonTagCompound data = newWorldConfig.getData();
        if (data != null) {
            data.putValue("LevelName", newWorldConfig.worldname);
            newWorldConfig.setData(data);
        }
        return true;
    }

    /**
     * Deletes this world from disk and removes all configuration associated with it
     * 
     * @return True if successful, False if not
     */
    public boolean deleteWorld() {
        if (this.isLoaded()) {
            return false;
        }
        File worldFolder = this.getWorldFolder();
        WorldConfig.remove(this.worldname);
        return StreamUtil.deleteFile(worldFolder).isEmpty();
    }

    @Override
    public String toString() {
        return "WorldConfig{world=" + worldname + "}";
    }

    /*
     * Bukkit ugh.
     */
    private static void setBukkitSpawn(World world, Location location) {
        if (CommonBootstrap.evaluateMCVersion(">=", "1.13")) {
            setBukkitSpawnWithLocationMethod(world, location);
        } else {
            setBukkitSpawnWithBlockXYZMethod(world, location);
        }
    }

    private static void setBukkitSpawnWithLocationMethod(World world, Location location) {
        world.setSpawnLocation(location);
    }

    private static void setBukkitSpawnWithBlockXYZMethod(World world, Location location) {
        world.setSpawnLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private class FindSafeSpawnTask extends Task {

        public FindSafeSpawnTask() {
            super(MyWorlds.plugin);
        }

        @Override
        public void run() {
            World world = getWorld();
            if (world == null) {
                return; // Unloaded already?
            }
            Block spawnPointBlock;
            if (spawnPoint != null) {
                spawnPointBlock = spawnPoint.toLocation(world).getBlock();
            } else {
                spawnPointBlock = world.getSpawnLocation().getBlock();
            }

            if (BlockUtil.isSuffocating(spawnPointBlock)) {
                fixSpawnLocation(world);
            }
        }
    }
}
