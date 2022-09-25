package com.bergerkiller.bukkit.mw.playerdata;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.WorldConfig;

import net.md_5.bungee.api.ChatColor;

/**
 * Performs (asynchronous) player data inventory migration.
 * Players whose data is being migrated cannot join the server until
 * the process is done.
 */
public class PlayerDataMigrator implements Listener {
    private final MyWorlds plugin;
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final AsyncTask migrationTask;
    private final List<UUID> pendingPlayerUUIDs = new ArrayList<>();
    private Consumer<String> task = uuid -> {};
    private String taskName = "";

    public PlayerDataMigrator(MyWorlds plugin) {
        this.plugin = plugin;
        this.migrationTask = new AsyncTask() {
            @Override
            public void run() {
                UUID uuid;
                synchronized (pendingPlayerUUIDs) {
                    int count = pendingPlayerUUIDs.size();
                    if (count == 0) {
                        task = u -> {};
                        migrationTask.stop();
                        busy.set(false);
                        return;
                    }
                    uuid = pendingPlayerUUIDs.remove(count - 1);
                }
                process(uuid);
            }
        };
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
        //plugin.changeMainWorld(newMainWorld.worldname);

        // Migrate all player data to move the main world information to the new world
        final File curMainWorldPlayerData = curMainWorld.getPlayerFolder();
        final File newMainWorldPlayerData = newMainWorld.getPlayerFolder();
        scheduleForWorlds("to a new Main World configuration", Arrays.asList(curMainWorld, newMainWorld), profileName -> {
            File curFile = new File(curMainWorldPlayerData, profileName);
            File newFile = new File(newMainWorldPlayerData, profileName);
            if (!curFile.exists()) {
                return;
            }

            // Try to load the player data files
            
            // If new file does not exist, create a blank slate storing only the main world details
            
            
            System.out.println("MIGRATE " + curFile + " / " + newFile);
        });
    }

    public void scheduleForWorlds(String name, Collection<WorldConfig> worlds, Consumer<String> task) {
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

    public void schedule(String name, Collection<UUID> uuids, Consumer<String> task) {
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
        this.migrationTask.start(true);
    }

    private void process(UUID uuid) {
        try {
            task.accept(uuid.toString() + ".dat");
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
            sender.sendMessage(ChatColor.GREEN + "Migrating " + taskName + " finsihed!");
        }
    }

    public void waitUntilFinished() {
        if (busy.get()) {
            plugin.log(Level.INFO, "Waiting for player data migration to finish...");
            migrationTask.waitFinished();
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
}
