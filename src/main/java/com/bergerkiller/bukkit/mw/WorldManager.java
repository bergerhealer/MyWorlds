package com.bergerkiller.bukkit.mw;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;

import com.bergerkiller.bukkit.common.chunk.ForcedChunk;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.mw.events.MyWorldsTeleportCommandEvent;
import com.bergerkiller.bukkit.mw.events.MyWorldsTeleportEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.bergerkiller.bukkit.common.nbt.CommonTagCompound;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.bukkit.common.utils.MaterialUtil;
import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.common.utils.StreamUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.common.wrappers.BlockData;
import com.bergerkiller.bukkit.common.wrappers.PlayerRespawnPoint;
import com.bergerkiller.bukkit.mw.utils.GeneratorStructuresParser;

public class WorldManager {

    private static boolean isSafeSpawnAir(BlockData blockData, Block block) {
        if (MaterialUtil.ISLIQUID.get(blockData)) {
            return false;
        }
        if (MaterialUtil.ISPRESSUREPLATE.get(blockData)) {
            return false;
        }
        if (blockData.isSuffocating(block) || blockData.isOccluding(block)) {
            return false;
        }
        return true;
    }

    private static boolean isSafeSpawnSurface(BlockData blockData, Block block) {
        if (Util.IS_END_PORTAL.get(blockData) || MaterialUtil.ISNETHERPORTAL.get(blockData)) {
            return false;
        }
        if (MaterialUtil.ISLEAVES.get(blockData)) {
            return false;
        }
        if (!blockData.isOccluding(block)) {
            return false;
        }
        return true;
    }

    /*
     * Get or set the respawn point of a world
     */
    public static void setSpawnLocation(String forWorld, Location destination) {
        setSpawn(forWorld, new Position(destination));
    }
    public static void setSpawn(String forWorld, Position destination) {
        WorldConfig.get(forWorld).setSpawnLocation(destination);
    }

    /*
     * Gets a possible teleport position on a certain world
     */
    public static Location getSpawnLocation(World onWorld) {
        return WorldConfig.get(onWorld).getSpawnLocation();
    }

    /**
     * Gets the Spawn Position of a World, which permits unloaded worlds to be checked
     * 
     * @param onWorldName of the World to get the spawn of
     * @return Spawn position
     */
    public static Position getSpawnPosition(String onWorldName) {
        return WorldConfig.get(onWorldName).tryFindSpawnPositionOffline();
    }

    /*
     * Chunk generators
     */
    public static String[] getGeneratorPlugins() {
        ArrayList<String> gens = new ArrayList<String>();
        for (Plugin plugin : Bukkit.getServer().getPluginManager().getPlugins()) {
            try {
                String mainclass = plugin.getDescription().getMain();
                Class<?> cmain = plugin.getClass();
                if (!cmain.getName().equals(mainclass)) {
                    // Hmm...
                    cmain = Class.forName(mainclass);
                }
                if (cmain.getMethod("getDefaultWorldGenerator", String.class, String.class).getDeclaringClass() != JavaPlugin.class) {
                    gens.add(plugin.getDescription().getName());
                }
            } catch (Throwable t) {}
        }
        return gens.toArray(new String[0]);
    }

    public static String fixGeneratorName(String name) {
        if (LogicUtil.nullOrEmpty(name)) {
            return null;
        }
        String genName = name;
        String args = "";
        int index = name.indexOf(":");
        if (index != -1) {
            args = name.substring(index + 1);
            genName = name.substring(0, index);
        }

        genName = fixGeneratorName_noargs(genName);
        if (genName == null) {
            return null;
        } else if (args.isEmpty()) {
            return genName;
        } else {
            return genName + ":" + args;
        }
    }

    private static String fixGeneratorName_noargs(String name) {
        // No generator
        if (name.isEmpty()) {
            return name;
        }

        // Name of a plugin
        final String pname = ParseUtil.parseArray(getGeneratorPlugins(), name.toLowerCase(Locale.ENGLISH), null);
        if (pname != null) {
            return pname;
        }

        // Not found
        return null;
    }

    public static String getGeneratorPluginName(String generatorNameWithOptions) {
        if (generatorNameWithOptions == null) {
            return null;
        }
        String name = generatorNameWithOptions;
        int index = name.indexOf(":");
        if (index != -1) {
            name = name.substring(0, index);
        }
        return name;
    }

    public static ChunkGenerator getGenerator(String worldname, String name) {
        if (name == null) {
            return null;
        }
        String id = "";
        int index = name.indexOf(":");
        if (index != -1) {
            id = name.substring(index + 1);
            name = name.substring(0, index);
        }
        Plugin plug = Bukkit.getServer().getPluginManager().getPlugin(name);
        if (plug != null) {
            // Eliminate ;nostructures from the options passed to the generator
            GeneratorStructuresParser parser = new GeneratorStructuresParser();
            id = parser.process(id);
            //TODO: Do we do anything with structures/nostructures, here?
            return plug.getDefaultWorldGenerator(worldname, id);
        } else {
            return null;
        }
    }

    /*
     * General world fields
     */
    public static long getSeed(String seed) {
        if (seed == null) return 0;
        long seedval = 0;
        try {
            seedval = Long.parseLong(seed);
        } catch (Exception ex) {
            seedval = seed.hashCode();
        }
        return seedval;
    }

    public static long getRandomSeed(String seed) {
        long seedval = getSeed(seed);
        if (seedval == 0) {
            seedval = new Random().nextLong();
        }
        return seedval;
    }

    public static String getWorldName(CommandSender sender, String[] args, boolean useAlternative) {
        String alternative = (args == null || args.length == 0) ? null : args[args.length - 1];
        return getWorldName(sender, alternative, useAlternative);
    }

    public static String getWorldName(CommandSender sender, String alternative, boolean useAlternative) {
        String worldname = null;
        if (useAlternative) {
            worldname = WorldManager.matchWorld(alternative);
        } else if (sender instanceof Player) {
            worldname = ((Player) sender).getWorld().getName();
        } else {
            for (World w : Bukkit.getServer().getWorlds()) {
                worldname = w.getName();
                break;
            }
        }
        return worldname;
    }

    public static String matchWorld(String matchname) {
        if (matchname == null || matchname.isEmpty()) {
            return null;
        }
        Collection<String> worldnames = WorldUtil.getLoadableWorlds();
        for (String worldname : worldnames) {
            if (worldname.equalsIgnoreCase(matchname)) return worldname;
        }
        matchname = matchname.toLowerCase();
        for (String worldname : worldnames) {
            if (worldname.toLowerCase().contains(matchname)) return worldname;
        }
        return null;
    }

    public static World getWorld(String worldname) {
        if (worldname == null) return null;
        try {
            return Bukkit.getServer().getWorld(worldname);
        } catch (Exception ex) {
            return null;
        }
    }

    public static boolean isLoaded(String worldname) {
        return getWorld(worldname) != null;
    }

    public static boolean worldExists(String worldname) {
        return worldname != null && WorldUtil.isLoadableWorld(worldname);
    }

    public static World getOrCreateWorld(String worldname) {
        World w = getWorld(worldname);
        if (w != null) return w;
        return createWorld(worldname, 0);
    }

    public static boolean unload(World world) {
        if (world != null) {
            LoadChunksTask.abortWorld(world, true);
            if (Bukkit.getServer().unloadWorld(world, world.isAutoSave())) {
                WorldUtil.closeWorldStreams(world);
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a new World
     * 
     * @param worldname to create
     * @param seed to use
     * @return The created World, or null on failure
     */
    public static World createWorld(String worldname, long seed) {
        return createWorld(worldname, seed, null);
    }

    /**
     * Creates a new World
     * 
     * @param worldname to create
     * @param seed to use, 0 does not override it (TODO: Fix this?)
     * @param sender to send creation problems to if they occur (null to ignore)
     * @return The created World, or null on failure
     */
    public static World createWorld(String worldname, long seed, CommandSender sender) {
        final MyWorlds plugin = MyWorlds.plugin;

        WorldConfig wc = WorldConfig.get(worldname);
        final boolean load = wc.isInitialized();
        String chunkGeneratorName = wc.getChunkGeneratorName();
        StringBuilder msg = new StringBuilder();
        if (load) {
            msg.append("Loading");
        } else {
            msg.append("Generating");
        }
        msg.append(" world '").append(worldname).append("'");
        if (seed == 0) {
            if (chunkGeneratorName != null) {
                msg.append(" using chunk generator: '").append(chunkGeneratorName).append("'");
            }
        } else {
            msg.append(" using seed ").append(seed);
            if (chunkGeneratorName != null) {
                msg.append(" and chunk generator: '").append(chunkGeneratorName).append("'");
            }
        }
        plugin.log(Level.INFO, msg.toString());

        World w = null;
        int i = 0;
        ChunkGenerator cgen = null;
        try {
            if (chunkGeneratorName != null) {
                cgen = getGenerator(worldname, chunkGeneratorName);
            }
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize generator " + chunkGeneratorName, t);
            if (sender != null) {
                sender.sendMessage(ChatColor.RED + "Failed to initialize generator " + chunkGeneratorName + ": " +
                        t.getMessage());
            }
            return null;
        }
        if (cgen == null) {
            if (chunkGeneratorName != null && chunkGeneratorName.indexOf(':') != 0) {
                msg.setLength(0);
                msg.append("World '").append(worldname);
                msg.append("' could not be created because the chunk generator '");
                msg.append(chunkGeneratorName).append("' was not found!");
                plugin.log(Level.SEVERE, msg.toString());
                if (sender != null) {
                    sender.sendMessage(ChatColor.RED + msg.toString());
                }
                return null;
            }
        }
        Throwable failReason = null;
        try {
            WorldCreator c = new WorldCreator(worldname);
            wc.worldmode.apply(c);
            if (seed != 0) {
                c.seed(seed);
            }

            // Parse args from chunkgenerator name
            String options = "";
            if (chunkGeneratorName != null) {
                int chunkGenArgsStart = chunkGeneratorName.indexOf(':');
                if (chunkGenArgsStart != -1) {
                    options = chunkGeneratorName.substring(chunkGenArgsStart + 1);
                }
            }

            // Parse custom structures/nostructures option from options
            if (!options.isEmpty()) {
                GeneratorStructuresParser parser = new GeneratorStructuresParser();
                options = parser.process(options);
                if (parser.hasNoStructures) {
                    c.generateStructures(false);
                } else if (parser.hasStructures) {
                    c.generateStructures(true);
                }
            }

            // Vanilla flat world settings
            if (!options.isEmpty() && cgen == null) {
                c.generatorSettings(options);
            } else {
                c.generatorSettings("{}");
            }

            c.generator(cgen);
            w = c.createWorld();
        } catch (Throwable t) {
            failReason = t;
        }
        if (w == null) {
            if (failReason != null) {
                plugin.getLogger().log(Level.SEVERE, "World creation failed after " + i + " retries!", failReason);
                if (sender != null) {
                    sender.sendMessage(ChatColor.RED + "Failed to create world: " + failReason.getMessage());
                }
            } else {
                plugin.log(Level.SEVERE, "World creation failed after " + i + " retries!");
            }
        } else if (i == 1) {
            plugin.log(Level.INFO, "World creation succeeded after 1 retry!");
        } else if (i > 0) {
            plugin.log(Level.INFO, "World creation succeeded after " + i + " retries!");
        }

        // Force a save of world configurations so this persists
        // The just-loaded world will have the configuration applied to it thanks to the WorldLoadEvent
        WorldConfigStore.saveAll(plugin);

        return w;
    }

    /**
     * Reads the generator-settings field in server.properties
     * 
     * @return default generator settings
     */
    public static String readDefaultGeneratorSettings() {
        return CommonUtil.getServerProperty("generator-settings", "");
    }

    public static Location getEvacuation(Player player) {
        World world = player.getWorld();
        String[] portalnames;
        if (Permission.COMMAND_SPAWN.has(player) || Permission.COMMAND_TPP.has(player)) {
            for (World loadedWorld : Bukkit.getWorlds()) {
                if (!Permission.canEnter(player, loadedWorld)) {
                    continue;
                }
                if (!WorldConfig.get(loadedWorld).checkPlayerLimit(player)) {
                    continue;
                }
                return WorldConfig.get(loadedWorld).getSpawnLocation();
            }
            portalnames = Portal.getPortals();
        } else {
            portalnames = Portal.getPortals(world);
        }
        for (String name : portalnames) {
            if (Permission.canEnterPortal(player, name)) {
                Location loc = Portal.getPortalLocation(name, player.getWorld().getName(), true, player);
                if (loc == null || loc.getWorld() == null || loc.getWorld() == world) {
                    continue;
                }
                if (Permission.canEnter(player, loc.getWorld())) {
                    return loc;
                }
            }
        }
        return null;
    }

    public static void evacuate(World world, String message) {
        for (Player player : world.getPlayers()) {
            Location loc = getEvacuation(player);
            if (loc != null) {
                MyWorldsTeleportCommandEvent event = new MyWorldsTeleportCommandEvent(
                        player,
                        MyWorldsTeleportCommandEvent.CommandType.EVACUATE,
                        loc);
                if (!CommonUtil.callEvent(event).isCancelled()) {
                    player.teleport(event.getTo());
                    player.sendMessage(ChatColor.RED + message);
                    continue;
                }
            }

            player.kickPlayer(message);
        }
    }
    public static void teleportToClonedWorld(World from, World cloned) {
        for (Player p : from.getPlayers().toArray(new Player[0])) {
            Location loc = p.getLocation();
            loc.setWorld(cloned);
            p.teleport(loc);
        }
    }

    /**
     * Removes a player respawn point for a player if bed respawn is disabled for the world
     * it is in.
     *
     * @param player
     */
    public static void removeInvalidBedSpawnPoint(Player player) {
        PlayerRespawnPoint respawn = PlayerRespawnPoint.forPlayer(player);
        World w;
        if (!respawn.isNone() && (w = respawn.getWorld()) != null && !WorldConfig.get(w).getBedRespawnMode().persistInProfile()) {
            PlayerRespawnPoint.NONE.applyToPlayer(player);
        }
    }

    /**
     * Tries to read player data to find a stored bed or world anchor respawn location for a Player
     * on another world.
     * 
     * @param player The player to find a bed spawn of
     * @param world The world on which to find a bed spawn
     * @return bed spawn location
     */
    public static Location getPlayerRespawnPosition(Player player, World world) {
        return MWPlayerDataController.readRespawnPoint(player, world).findSafeSpawn();
    }

    /**
     * Attempts to let a player rejoin a world, or group of worlds, a player was on
     * based on the world configuration 'rejoin group'. If the player has no known previous
     * world/position information for the world, then the player rejoins the
     * world parameter instead.
     *
     * @param player Player to find a rejoin location for
     * @param world World to rejoin
     * @return Rejoined location
     */
    public static Location getPlayerRejoinPosition(Player player, World world) {
        // Check rejoin group rules to find what group this world is a part of
        // If found, and loaded, use that group's defined main world to find the spawn
        WorldConfig worldConfig = WorldConfig.get(world);
        WorldConfig mainConfig = WorldConfigStore.findRejoin(worldConfig);
        {
            World w = mainConfig.getWorld();
            if (w != null) {
                world = w;
            }
        }

        // If player is already currently on a world part of the world group,
        // just return the player's current position
        List<WorldConfig> groupWorlds = mainConfig.getRejoinGroupWorldConfigs();
        if (groupWorlds.contains(WorldConfig.get(player.getWorld()))) {
            return player.getLocation();
        }

        // Check player data what the last world was the player was on
        // Return the position on that world
        Location rejoinedLoc = MWPlayerDataController.readLastLocationOfWorldGroup(player, groupWorlds);
        if (rejoinedLoc != null) {
            return rejoinedLoc;
        }

        // Try a bed spawn
        Location bedSpawnLocation = getPlayerRespawnPosition(player, world);
        if (bedSpawnLocation != null) {
            return bedSpawnLocation;
        }

        // World spawn point
        return getSpawnLocation(world);
    }

    /**
     * Tries to find a (personal) spawn location to teleport players to.
     * If last position remembering is turned on and a last position is known, 
     * this Location is returned. If lacking, and the player has a bed spawn there,
     * that one is used. Otherwise, the world spawn point is returned.
     *
     * @param player Player to find the world spawn for
     * @param world World to find the world spawn in
     * @param playerRespawnPoint Whether to use the player respawn point (bed) if available
     * @return Spawn location
     */
    public static WorldSpawnLocation getPlayerWorldSpawn(Player player, World world, boolean playerRespawnPoint) {
        if (WorldConfig.get(world).isTeleportingToLastPosition(player)) {
            // Player is already in the world to go to...so we just return that instead
            if (player.getWorld() == world) {
                return new WorldSpawnLocation(player.getLocation(), WorldSpawnLocation.Type.LAST_POSITION);
            }

            // Figure out the last position of the player on the world
            Location location = MWPlayerDataController.readLastLocation(player, world);
            if (location != null) {
                return new WorldSpawnLocation(location, WorldSpawnLocation.Type.LAST_POSITION);
            }
        }

        // Try a bed spawn
        if (playerRespawnPoint) {
            Location bedSpawnLocation = getPlayerRespawnPosition(player, world);
            if (bedSpawnLocation != null) {
                return new WorldSpawnLocation(bedSpawnLocation, WorldSpawnLocation.Type.BED_SPAWN);
            }
        }

        // World spawn point
        return new WorldSpawnLocation(getSpawnLocation(world), WorldSpawnLocation.Type.WORLD_SPAWN);
    }

    /**
     * Tries to deduce the current world an OfflinePlayer is or was on. If the player
     * is currently online, returns the live current world. If not, the world is checked
     * using the player's saved data. If none is available, the main spawn world is
     * returned here.
     *
     * @param player Player profile to read the world of
     * @return World this player was on
     */
    public static World getPlayerCurrentWorld(OfflinePlayer player) {
        // Easy
        if (player instanceof Player) {
            return ((Player) player).getWorld();
        }

        // Try and find using the player data controller
        return MWPlayerDataController.findPlayerWorld(player);
    }

    /**
     * Stores the spawn location information of a player
     */
    public static class WorldSpawnLocation {
        public final Location location;
        public final Type type;

        public WorldSpawnLocation(Location location, Type type) {
            this.location = location;
            this.type = type;
        }

        /**
         * Asynchronously loads the destination, then attempts to teleport the player to it. Relevant events
         * are fired before teleporting starts, which might cause the single chunk at the location to load
         * synchronously.
         *
         * @param player Player to teleport
         * @param eventFactory Handles a myworlds teleport event before teleportation commences
         * @return Future completed with True if successful, or False if not.
         */
        public CompletableFuture<Boolean> teleportAsync(Player player, MyWorldsTeleportEvent.Factory eventFactory) {
            if (type == WorldSpawnLocation.Type.LAST_POSITION) {
                return teleportToExactAsync(player, location, eventFactory);
            }

            Location loc = location.clone();
            if (eventFactory != null) {
                MyWorldsTeleportEvent event = eventFactory.create(player, loc);
                if (event != null) {
                    if (CommonUtil.callEvent(event).isCancelled()) {
                        return CompletableFuture.completedFuture(Boolean.FALSE);
                    }
                    loc = event.getTo();
                }
            }

            final Location locCopy = loc;
            return loadChunksAsync(locCopy, 3).thenApply(u -> EntityUtil.teleport(player, locCopy));
        }

        /**
         * Teleports a player to this spawn location
         *
         * @param player Player to teleport
         * @param eventFactory Handles a myworlds teleport event before teleportation commences
         * @return True if successful, False if not
         */
        public boolean teleport(Player player, MyWorldsTeleportEvent.Factory eventFactory) {
            if (type == WorldSpawnLocation.Type.LAST_POSITION) {
                return teleportToExact(player, location, eventFactory);
            }

            Location loc = location.clone();
            if (eventFactory != null) {
                MyWorldsTeleportEvent event = eventFactory.create(player, loc);
                if (event != null) {
                    if (CommonUtil.callEvent(event).isCancelled()) {
                        return false;
                    }
                    loc = event.getTo();
                }
            }
            return EntityUtil.teleport(player, loc);
        }

        /**
         * Type of world spawn location
         */
        public static enum Type {
            /** Last known position of the player on the world */
            LAST_POSITION,
            /** Bed spawn or world anchor spawn */
            BED_SPAWN,
            /** World spawn point */
            WORLD_SPAWN
        }
    }

    /**
     * Asynchronously loads the destination, then performs the same logic as
     * {@link #teleportToExact(Entity, Location)}
     *
     * @param entity The entity to teleport
     * @param destination Destination location to teleport to
     * @return Future completed with True if the teleport succeeded, False if not
     */
    public static CompletableFuture<Boolean> teleportToExactAsync(Entity entity, Location destination) {
        return teleportToExactAsync(entity, destination, null);
    }

    /**
     * Asynchronously loads the destination, then performs the same logic as
     * {@link #teleportToExact(Entity, Location, MyWorldsTeleportEvent.Factory)}
     *
     * @param entity The Entity to teleport
     * @param destination Destination location to teleport to
     * @param eventFactory Handles a myworlds teleport event before teleportation commences
     * @return Future completed with True if the teleport succeeded, False if not
     */
    public static CompletableFuture<Boolean> teleportToExactAsync(Entity entity, Location destination, MyWorldsTeleportEvent.Factory eventFactory) {
        destination = destination.clone(); // Safety, people can muck with teleport events

        if (eventFactory != null) {
            MyWorldsTeleportEvent event = eventFactory.create(entity, destination);
            if (event != null) {
                if (CommonUtil.callEvent(event).isCancelled()) {
                    return CompletableFuture.completedFuture(Boolean.FALSE);
                }
                destination = event.getTo();
            }
        }

        final Location destinationCopy = destination;
        return loadChunksAsync(destination, 3).thenApply(u -> {
            if (entity.isDead() || (entity instanceof Player && !((Player) entity).isValid())) {
                return false;
            }

            boolean crossWorlds = (destinationCopy.getWorld() != entity.getWorld());

            if (!EntityUtil.teleport(entity, destinationCopy)) {
                return false;
            }
            if (crossWorlds &&
                    entity.getWorld() == destinationCopy.getWorld() &&
                    !entity.getLocation().equals(destinationCopy)
            ) {
                entity.teleport(destinationCopy);
            }
            return true;
        });
    }

    /**
     * Teleports a player to a destination location. If the location is solid,
     * and the teleportation is cross-world, normally the player is teleported
     * away to a safe place. If this happens, a second teleport is done to
     * put the player back at the 'unsafe' location.
     *
     * This extra logic is important when players rejoin a world at their
     * last-known location, otherwise certain block glitches allow them to
     * teleport upwards.
     *
     * @param entity The entity to teleport
     * @param destination Destination location to teleport to
     * @return True if the teleport succeeded, False if not
     */
    public static boolean teleportToExact(Entity entity, Location destination) {
        return teleportToExact(entity, destination, null);
    }

    /**
     * Teleports a player to a destination location. If the location is solid,
     * and the teleportation is cross-world, normally the player is teleported
     * away to a safe place. If this happens, a second teleport is done to
     * put the player back at the 'unsafe' location.
     *
     * This extra logic is important when players rejoin a world at their
     * last-known location, otherwise certain block glitches allow them to
     * teleport upwards.
     *
     * @param entity The entity to teleport
     * @param destination Destination location to teleport to
     * @param eventFactory Handles a myworlds teleport event before teleportation commences
     * @return True if the teleport succeeded, False if not
     */
    public static boolean teleportToExact(Entity entity, Location destination, MyWorldsTeleportEvent.Factory eventFactory) {
        destination = destination.clone(); // Safety, people can muck with teleport events

        if (eventFactory != null) {
            MyWorldsTeleportEvent event = eventFactory.create(entity, destination);
            if (event != null) {
                if (CommonUtil.callEvent(event).isCancelled()) {
                    return false;
                }
                destination = event.getTo();
            }
        }

        boolean crossWorlds = (destination.getWorld() != entity.getWorld());
        if (!EntityUtil.teleport(entity, destination)) {
            return false;
        }
        if (crossWorlds &&
                entity.getWorld() == destination.getWorld() &&
                !entity.getLocation().equals(destination)
        ) {
            entity.teleport(destination);
        }
        return true;
    }

    /**
     * Teleports a player to a world. If the world allows the last player position
     * on the world to be used, this is used instead. If no last position is known,
     * or last position remembering is disabled, the player is teleported to the
     * world spawn position.
     * 
     * @param player to teleport
     * @param world to teleport to
     * @return True if successful, False if not
     */
    public static boolean teleportToWorld(Player player, World world) {
        return teleportToWorld(player, world, null);
    }

    /**
     * Teleports a player to a world. If the world allows the last player position
     * on the world to be used, this is used instead. If no last position is known,
     * or last position remembering is disabled, the player is teleported to the
     * world spawn position.
     *
     * @param player to teleport
     * @param world to teleport to
     * @param eventFactory Handles a myworlds teleport event before teleportation commences
     * @return True if successful, False if not
     */
    public static boolean teleportToWorld(Player player, World world, MyWorldsTeleportEvent.Factory eventFactory) {
        return getPlayerWorldSpawn(player, world, false)
                .teleport(player, eventFactory);
    }

    /**
     * Teleports all players from one world to the other.
     * 
     * @param from world to teleport all players from
     * @param to world to teleport to
     * @see #teleportToWorld(Player, World)
     */
    public static void teleportAllPlayersToWorld(World from, World to) {
        for (Player p : from.getPlayers().toArray(new Player[0])) {
            teleportToWorld(p, to);
        }
    }

    /**
     * Same as {@link WorldUtil#loadChunks(Location, int)} but asynchronous
     *
     * @param center Center location
     * @param chunkRadius Radius around center chunk in chunk size
     * @return Future completed when chunks are loaded, completed on main thread.
     *         After handling the chunks are allowed to unload again.
     */
    public static CompletableFuture<Void> loadChunksAsync(Location center, int chunkRadius) {
        // Start loading all chunks asynchronously
        List<ForcedChunk> chunks = new ArrayList<>((chunkRadius*2+1) * (chunkRadius*2+1));
        int xmid = MathUtil.toChunk(center.getX());
        int zmid = MathUtil.toChunk(center.getZ());
        for(int cx = xmid - chunkRadius; cx <= xmid + chunkRadius; ++cx) {
            for(int cz = zmid - chunkRadius; cz <= zmid + chunkRadius; ++cz) {
                chunks.add(WorldUtil.forceChunkLoaded(center.getWorld(), cx, cz));
            }
        }

        // Collect all futures and do an all() on it
        CompletableFuture<Void> whenLoaded = CompletableFuture.allOf(chunks.stream()
                .map(ForcedChunk::getChunkAsync)
                .toArray(CompletableFuture[]::new));

        final CompletableFuture<Void> future = new CompletableFuture<>();
        whenLoaded.handle((v, err) -> {
            try {
                if (err != null) {
                    future.completeExceptionally(err);
                } else {
                    future.complete(v);
                }
            } finally {
                chunks.forEach(ForcedChunk::close);
                chunks.clear();
            }
            return null;
        });
        return future;
    }

    /**
     * Looks for a suitable place to spawn near the Start Location specified.
     * 
     * @param startLocation
     * @return A safe location to spawn at
     */
    public static Location getSafeSpawn(Location startLocation) {
        // Do it ourselves
        final World world = startLocation.getWorld();
        final int blockX = startLocation.getBlockX();
        final int blockY = startLocation.getBlockY();
        final int blockZ = startLocation.getBlockZ();
        final int startY = world.getEnvironment() == Environment.NETHER ? 127 : (world.getMaxHeight() - 1);
        final Random random = new Random();
        int y, x = blockX, z = blockZ;
        int alterX = blockX, alterY = blockY, alterZ = blockZ;
        int airCounter;
        BlockData data;
        Block block;
        for (int retry = 0; retry < 200; retry++) {
            airCounter = world.getEnvironment() == Environment.NETHER ? 0 : 2;
            for (y = startY; y > 0; y--) {
                block = world.getBlockAt(x, y, z);
                data = WorldUtil.getBlockData(block);
                if (isSafeSpawnAir(data, block)) {
                    // Air block - continue
                    airCounter++;
                } else {
                    // Safe spawn is only there with 2 air-blocks above the surface
                    if (airCounter >= 2) {
                        if (isSafeSpawnSurface(data, block)) {
                            // Safe place to spawn on - ENDING
                            return new Location(world, x + 0.5, y + 1.5, z + 0.5);
                        } else {
                            // Unsafe place but still a surface...set as alternative
                            alterX = x;
                            alterY = y + 1;
                            alterZ = z;
                            break;
                        }
                    }
                    airCounter = 0;
                }
            }

            // Obtain a new column to look at
            // For the first 5 calls, probe around a bit < 8 blocks away
            if (retry <= 5) {
                x = blockX + random.nextInt(8) - 3;
                z = blockZ + random.nextInt(8) - 3;
            } else {
                x = blockX + random.nextInt(128) - 63;
                z = blockZ + random.nextInt(128) - 63;
            }
        }
        // Return the alternative (unsafe) location
        // This could occur if spawning in an ocean, for example
        return new Location(world, alterX + 0.5, alterY + 0.5, alterZ + 0.5);
    }

    /**
     * Repairs the chunk region file
     * Returns -1 if the file had to be removed
     * Returns -2 if we had no access
     * Returns -3 if file removal failed (from -1)
     * Returns the amount of changed chunks otherwise
     * @param chunkfile
     * @param backupfolder
     * @return
     */
    public static int repairRegion(File chunkfile, File backupfolder) {
        MyWorlds.plugin.log(Level.INFO, "Performing repairs on region file: " + chunkfile.getName());
        RandomAccessFile raf = null;
        try {
            String name = chunkfile.getName().substring(2);
            String xname = name.substring(0, name.indexOf('.'));
            name = name.substring(xname.length() + 1);
            String zname = name.substring(0, name.indexOf('.'));
            
            int xOffset = 32 * Integer.parseInt(xname);
            int zOffset = 32 * Integer.parseInt(zname);
            
            raf = new RandomAccessFile(chunkfile, "rw");
            File backupfile = new File(backupfolder, chunkfile.getName());
            int[] locations = new int[1024];
            for (int i = 0; i < 1024; i++) {
                locations[i] = raf.readInt();
            }
            //Validate the data
            int editcount = 0;
            int chunkX = 0;
            int chunkZ = 0;
            
            //x = 5
            //z = 14
            //i = 5 + 14 * 32 = 453
            //x = i % 32 = 5
            //z = [(453 - 5)]  448 / 32 = 
            
            byte[] data = new byte[8096];
            for (int i = 0; i < locations.length; i++) {
                chunkX = i % 32;
                chunkZ = (i - chunkX) >> 5;
                chunkX += xOffset;
                chunkZ += zOffset;
                int location = locations[i];
                if (location == 0) {
                    continue;
                }
                try {
                    int offset = location >> 8;
                    int size = location & 255;
                    long seekindex = (long) (offset * 4096);
                    raf.seek(seekindex);
                    int length = raf.readInt();
                    //Read and test the data
                    if (length > 4096 * size) {
                        editcount++;
                        locations[i] = 0;
                        MyWorlds.plugin.log(Level.WARNING, "Invalid length: " + length + " > 4096 * " + size);
                        //Invalid length
                    } else if (size > 0 && length > 0) {
                        byte version = raf.readByte();
                        if (data.length < length + 10) {
                            data = new byte[length + 10];
                        }
                        raf.read(data, 0, length - 1);
                        ByteArrayInputStream bais = new ByteArrayInputStream(data, 0, length - 1);
                        //Try to load it all...
                        DataInputStream stream;
                        if (version == 1) {
                            stream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(bais)));
                        } else if (version == 2) {
                            stream = new DataInputStream(new BufferedInputStream(new InflaterInputStream(bais)));
                        } else {
                            stream = null;
                            //Unknown version
                            MyWorlds.plugin.log(Level.WARNING, "Unknown region version: " + version + " (we probably need an update here!)");
                        }
                        if (stream != null) {
                            
                            //Validate the stream and close
                            try {
                                CommonTagCompound comp = CommonTagCompound.readFromStream(stream, false);
                                if (comp == null) {
                                    editcount++;
                                    locations[i] = 0;
                                    MyWorlds.plugin.log(Level.WARNING, "Invalid tag compound at chunk " + chunkX + "/" + chunkZ);
                                } else {
                                    //correct location?
                                    if (comp.containsKey("Level")) {
                                        CommonTagCompound level = comp.createCompound("Level");
                                        int xPos = level.getValue("xPos", Integer.MIN_VALUE);
                                        int zPos = level.getValue("zPos", Integer.MIN_VALUE);
                                        //valid coordinates?
                                        if (xPos != chunkX || zPos != chunkZ) {
                                            MyWorlds.plugin.log(Level.WARNING, "Chunk [" + xPos + "/" + zPos + "] was stored at [" + chunkX + "/" + chunkZ + "], moving...");
                                            level.putValue("xPos", chunkX);
                                            level.putValue("zPos", chunkZ);
                                            //rewrite to stream
                                            ByteArrayOutputStream baos = new ByteArrayOutputStream(8096);
                                            OutputStream outputstream = new DeflaterOutputStream(baos);
                                            level.writeToStream(outputstream, false);
                                            outputstream.close();
                                            //write to region file
                                            raf.seek(seekindex);
                                            byte[] newdata = baos.toByteArray();
                                            raf.writeInt(newdata.length + 1);
                                            raf.writeByte(2);
                                            raf.write(newdata, 0, newdata.length);
                                            editcount++;
                                        }
                                    } else {
                                        editcount++;
                                        locations[i] = 0;
                                        MyWorlds.plugin.log(Level.WARNING, "Invalid tag compound at chunk " + chunkX + "/" + chunkZ);
                                    }
                                }
                            } catch (ZipException ex) {
                                //Invalid.
                                editcount++;
                                locations[i] = 0;
                                MyWorlds.plugin.log(Level.WARNING, "Chunk at position " + chunkX + "/" + chunkZ + " is not in a valid ZIP format (it's corrupted, and thus lost)");
                            } catch (Exception ex) {
                                //Invalid.
                                editcount++;
                                locations[i] = 0;
                                MyWorlds.plugin.getLogger().log(Level.WARNING, "Failed to properly read chunk at position " + chunkX + "/" + chunkZ, ex);
                            }
                            stream.close();
                        }
                    }
                } catch (Throwable t) {
                    editcount++;
                    locations[i] = 0;
                    MyWorlds.plugin.getLogger().log(Level.SEVERE, "Unhandled chunk error while handling world repair", t);
                }
            }
            if (editcount > 0) {
                if ((backupfolder.exists() || backupfolder.mkdirs()) && StreamUtil.tryCopyFile(chunkfile, backupfile)) {
                    //Write out the new locations
                    raf.seek(0);
                    for (int location : locations) {
                        raf.writeInt(location);
                    }
                } else {
                    MyWorlds.plugin.log(Level.WARNING, "Failed to make a copy of the file, no changes are made.");
                    raf.close();
                    return -2;
                }
            }
            //Done.
            raf.close();
            return editcount;
        } catch (IOException e) {
            MyWorlds.plugin.getLogger().log(Level.SEVERE, "Unhandled IO error while handling world region repair", e);
        }

        try {
            if (raf != null) raf.close();
        } catch (Exception ex) {}

        try {
            chunkfile.delete();
            return -1;
        } catch (Exception ex) {
            return -3;
        }
    }

    @Deprecated
    public static boolean generateData(String worldname, String seed) {
        return WorldConfig.get(worldname).resetData(seed);
    }

    @Deprecated
    public static boolean generateData(String worldname, long seed) {
        return WorldConfig.get(worldname).resetData(seed);
    }

    @Deprecated
    public static CommonTagCompound createData(String worldname, long seed) {
        return WorldConfig.get(worldname).createData(seed);
    }

    @Deprecated
    public static CommonTagCompound getData(String worldname) {
        return WorldConfig.get(worldname).getData();
    }

    @Deprecated
    public static boolean setData(String worldname, CommonTagCompound data) {
        return WorldConfig.get(worldname).setData(data);
    }

    @Deprecated
    public static File getDataFile(String worldname) {
        return WorldConfig.get(worldname).getDataFile();
    }

    @Deprecated
    public static File getUIDFile(String worldname) {
        return WorldConfig.get(worldname).getUIDFile();
    }

    @Deprecated
    public static long getWorldSize(String worldname) {
        return WorldConfig.get(worldname).getWorldSize();
    }

    @Deprecated
    public static WorldInfo getInfo(String worldname) {
        return WorldConfig.get(worldname).getInfo();
    }

    @Deprecated
    public static boolean isBroken(String worldname) {
        return WorldConfig.get(worldname).isBroken();
    }

    @Deprecated
    public static boolean isInitialized(String worldname) {
        return WorldConfig.get(worldname).isInitialized();
    }

    @Deprecated
    public static boolean copyWorld(String worldname, String newname) {
        return WorldConfig.get(worldname).copyTo(WorldConfig.get(newname));
    }

    @Deprecated
    public static boolean deleteWorld(String worldname) {
        return WorldConfig.get(worldname).deleteWorld();
    }
}