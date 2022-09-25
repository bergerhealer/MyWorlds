package com.bergerkiller.bukkit.mw.playerdata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.nbt.CommonTagCompound;
import com.bergerkiller.bukkit.common.nbt.CommonTagList;
import com.bergerkiller.bukkit.mw.MWPlayerDataController;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.WorldConfig;

/**
 * Performs (asynchronous) player data inventory migration.
 * Players whose data is being migrated cannot join the server until
 * the process is done.
 */
public class PlayerDataMigrator implements Listener {
    private final MyWorlds plugin;
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private AsyncTask migrationTask = null;
    private final List<UUID> pendingPlayerUUIDs = new ArrayList<>();
    private Migrator task = uuid -> {};
    private String taskName = "";

    public PlayerDataMigrator(MyWorlds plugin) {
        this.plugin = plugin;
    }

    /**
     * Changes the main world. The main world stores what world a player was last
     * on.
     *
     * @param newMainWorld New main world to change to
     */
    public void changeMainWorld(WorldConfig newMainWorld) {
        // Protect against this
        WorldConfig curMainWorld = WorldConfig.get(MyWorlds.getMainWorld());
        if (curMainWorld == newMainWorld) {
            return;
        }

        // Rewrite MyWorlds' config.yml to change the main world to the new one
        plugin.changeMainWorld(newMainWorld.worldname);

        // Migrate all player data to move the main world information to the new world
        final File curMainWorldPlayerData = curMainWorld.getPlayerFolder();
        final File newMainWorldPlayerData = newMainWorld.getPlayerFolder();
        scheduleForWorlds("to a new Main World configuration", Arrays.asList(curMainWorld, newMainWorld), playerUUID -> {
            CommonTagCompound curData = PlayerDataFile.readIfExists(plugin, curMainWorldPlayerData, playerUUID);
            CommonTagCompound newData = PlayerDataFile.readIfExists(plugin, newMainWorldPlayerData, playerUUID);

            // If current data is not available, there is nothing to migrate...
            if (curData == null) {
                return;
            }

            // If current data is self-contained, that means the data was saved before MyWorlds multi-world-inventories
            // was enabled as a feature. Simply copy the file to the new location and be done with it.
            if (!MyWorlds.useWorldInventories || PlayerDataFile.isSelfContained(curData)) {
                // Preserve legacy data tracked in this world before
                if (newData != null) {
                    // Note: if absent in data, puts null, which removes the tag
                    curData.put(MWPlayerDataController.LEGACY_DATA_TAG_LASTPOS,
                            newData.get(MWPlayerDataController.LEGACY_DATA_TAG_LASTPOS));
                    curData.put(MWPlayerDataController.LEGACY_DATA_TAG_LASTROT,
                            newData.get(MWPlayerDataController.LEGACY_DATA_TAG_LASTROT));
                    curData.put(MWPlayerDataController.LEGACY_DATA_TAG_LASTWORLD,
                            newData.get(MWPlayerDataController.LEGACY_DATA_TAG_LASTWORLD));
                } else {
                    // No data prior, so get rid of all these
                    curData.remove(MWPlayerDataController.LEGACY_DATA_TAG_LASTPOS);
                    curData.remove(MWPlayerDataController.LEGACY_DATA_TAG_LASTROT);
                    curData.remove(MWPlayerDataController.LEGACY_DATA_TAG_LASTWORLD);
                }

                // Copy current data to the new data file. Overwrite existing.
                PlayerDataFile.write(curData, newMainWorldPlayerData, playerUUID);
                // We can keep the original main world file. No big deal.
                return;
            }

            // Parse location history of the player stored in the original file
            // Clean up the MyWorlds data tag compound
            LastPlayerPositionList lastPositions = null;
            {
                CommonTagCompound myworldsData = curData.get(MWPlayerDataController.DATA_TAG_ROOT, CommonTagCompound.class);
                if (myworldsData != null) {
                    CommonTagList result = myworldsData.get(MWPlayerDataController.DATA_TAG_LAST_POSITIONS, CommonTagList.class);
                    if (result != null) {
                        lastPositions = new LastPlayerPositionList(result);

                        // Remove tag list from the original data
                        myworldsData.remove(MWPlayerDataController.DATA_TAG_LAST_POSITIONS);
                    }

                    // This is no longer relevant
                    myworldsData.remove(MWPlayerDataController.DATA_TAG_IS_SELF_CONTAINED);

                    // If now empty, clean up the MyWorlds node itself, too
                    if (myworldsData.isEmpty()) {
                        curData.remove(MWPlayerDataController.DATA_TAG_ROOT);
                    }
                }
            }

            // Store important main-world information in new world data
            newData.putUUID("World", curData.getUUID("World"));
            {
                CommonTagCompound myworldsData = newData.createCompound(MWPlayerDataController.DATA_TAG_ROOT);
                myworldsData.putValue(MWPlayerDataController.DATA_TAG_IS_SELF_CONTAINED, false);
                if (lastPositions == null) {
                    myworldsData.remove(MWPlayerDataController.DATA_TAG_LAST_POSITIONS);
                } else {
                    myworldsData.put(MWPlayerDataController.DATA_TAG_LAST_POSITIONS, lastPositions.getDataTag());
                }
            }

            // Write out the data to the new files
            PlayerDataFile.write(curData, curMainWorldPlayerData, playerUUID);
            PlayerDataFile.write(newData, newMainWorldPlayerData, playerUUID);
        });
    }

    public void scheduleForWorlds(String name, Collection<WorldConfig> worlds, Migrator task) {
        Map<UUID, Long> uuids = new HashMap<>(100);
        for (WorldConfig config : worlds) {
            File playerDataFolder = config.getPlayerFolder();
            if (playerDataFolder.exists()) {
                for (File file : playerDataFolder.listFiles()) {
                    String fileName = file.getName().toLowerCase(Locale.ENGLISH);
                    if (fileName.endsWith(".dat")) {
                        // Decode UUID
                        UUID uuid;
                        try {
                            uuid = UUID.fromString(fileName.substring(0, fileName.length() - 4));
                        } catch (IllegalArgumentException ex) {
                            /* ignore */
                            continue;
                        }

                        // Track latest modification timestamp of a player profile per uuid
                        final long lastModified;
                        {
                            long tmp = 0L;
                            try {
                                tmp = file.lastModified();
                            } catch (Throwable t) { /* meh */ }
                            lastModified = tmp;
                        }

                        // Map latest modification date to uuid
                        uuids.compute(uuid, (u, curr) -> {
                            if (curr != null && curr > lastModified) {
                                return curr;
                            } else {
                                return lastModified;
                            }
                        });
                    }
                }
            }
        }

        // Sort so the most recently changed UUID is at the back
        // We process asynchronously in reverse
        ArrayList<UUID> uuidsList = new ArrayList<>(uuids.keySet());
        uuidsList.sort((a, b) -> Long.compare(uuids.get(a), uuids.get(b)));

        schedule(name, uuidsList, task);
    }

    public void schedule(String name, Collection<UUID> uuids, Migrator task) {
        if (this.busy.get()) {
            return; // Safety!
        }

        this.taskName = name;
        this.task = task;
        synchronized (this.pendingPlayerUUIDs) {
            this.pendingPlayerUUIDs.clear();

            // Process UUIDs of players that are already logged on right away
            // Schedule offline players
            for (UUID uuid : uuids) {
                if (Bukkit.getServer().getPlayer(uuid) != null) {
                    process(uuid);
                } else {
                    this.pendingPlayerUUIDs.add(uuid);
                }
            }

            // If no tasks scheduled, don't even start the async task
            if (this.pendingPlayerUUIDs.isEmpty()) {
                return;
            }
        }
        this.busy.set(true);

        this.migrationTask = new AsyncTask() {
            @Override
            public void run() {
                UUID uuid;
                synchronized (pendingPlayerUUIDs) {
                    int count = pendingPlayerUUIDs.size();
                    if (count == 0) {
                        PlayerDataMigrator.this.task = u -> {};
                        migrationTask.stop();
                        migrationTask = null;
                        busy.set(false);
                        return;
                    }
                    uuid = pendingPlayerUUIDs.remove(count - 1);
                }
                process(uuid);
            }
        };
        this.migrationTask.start(true);
    }

    private void process(UUID uuid) {
        try {
            task.migrate(uuid);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Failed to process player profile of player uuid=" + uuid, t);
        }
    }

    public boolean isRunning() {
        return busy.get();
    }

    public void showStatus(CommandSender sender) {
        if (busy.get()) {
            synchronized (pendingPlayerUUIDs) {
                sender.sendMessage(ChatColor.YELLOW + "Migrating " + ChatColor.WHITE + taskName +
                        ChatColor.YELLOW + ", " + ChatColor.WHITE + pendingPlayerUUIDs.size() +
                        ChatColor.YELLOW + " player data profiles remaining");
            }
        } else if (taskName.isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + "No player data migration is scheduled");
        } else {
            sender.sendMessage(ChatColor.GREEN + "Migrating " + taskName + " finished!");
        }
    }

    public void waitUntilFinished() {
        AsyncTask currTask;
        if (busy.get() && ((currTask = migrationTask) != null)) {
            plugin.log(Level.INFO, "Waiting for player data migration to finish...");
            currTask.waitFinished();
            plugin.log(Level.INFO, "Player data migration finished!");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (busy.get()) {
            synchronized (pendingPlayerUUIDs) {
                if (pendingPlayerUUIDs.contains(event.getPlayer().getUniqueId())) {
                    event.setResult(Result.KICK_OTHER);
                    event.setKickMessage("Your player data is being migrated, try again later");
                }
            }
        }
    }

    @FunctionalInterface
    public static interface Migrator {
        public void migrate(UUID playerUUID) throws IOException;
    }
}
