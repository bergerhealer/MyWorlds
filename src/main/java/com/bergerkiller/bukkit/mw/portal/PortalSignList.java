package com.bergerkiller.bukkit.mw.portal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.io.AsyncTextWriter;
import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.Position;

/**
 * Stores the locations of all the portals on the server.
 * Changes are automatically synchronized to the portals.txt file.
 */
public class PortalSignList {
    private static final int AUTOSAVE_INTERVAL = 100;
    private final Map<String, Map<String, Position>> portals;
    private final File saveFile;
    private final MyWorlds plugin;
    private final Task autosaveTask;
    private boolean autosaveScheduled;
    private CompletableFuture<Void> saveFuture;

    public PortalSignList(MyWorlds plugin) {
        this.plugin = plugin;
        this.portals = new HashMap<>();
        this.saveFile = plugin.getDataFile("portals.txt");
        this.autosaveScheduled = false;
        this.saveFuture = CompletableFuture.completedFuture(null);
        this.autosaveTask = new Task(plugin) {
            @Override
            public void run() {
                // If the save isn't done yet, delay saving
                if (!saveFuture.isDone()) {
                    autosaveTask.start(AUTOSAVE_INTERVAL);
                    return;
                }

                autosaveScheduled = false;
                save();
            }
        };
    }

    public void enable() {
        load();
    }

    public void disable() {
        autosaveTask.stop();
        this.autosaveScheduled = false;

        // Wait until a previous save is done
        try {
            this.saveFuture.get();
        } catch (InterruptedException | ExecutionException e) {}

        // Perform the save
        save();

        // Wait for the save to complete (blocking)
        try {
            this.saveFuture.get();
        } catch (InterruptedException | ExecutionException e) {}
    }

    /**
     * Removes a portal from this store
     * 
     * @param portalName
     * @param worldName
     * @return True if the portal was removed
     */
    public boolean removePortal(String portalName, String worldName) {
        Map<String, Position> onWorld = this.portals.get(worldName.toLowerCase(Locale.ENGLISH));
        if (onWorld != null && onWorld.remove(portalName) != null) {
            scheduleSave();
            return true;
        }
        return false;
    }

    /**
     * Stores a portal under a name. It is mapped at the world name
     * declared in the position.
     * 
     * @param portalName
     * @param position
     */
    public void storePortal(String portalName, Position position) {
        this.portals.computeIfAbsent(position.getWorldName().toLowerCase(Locale.ENGLISH), n -> new HashMap<>())
                .put(portalName, position);
        scheduleSave();
    }

    /**
     * Gets a list of all portals and their portal position on the server
     * 
     * @return list of portalname - portalposition entries
     */
    public Collection<Map.Entry<String, Position>> listAllPortals() {
        return this.portals.values().stream()
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toList());
    }

    /**
     * Gets a mapping of all the portals on a world
     * 
     * @param worldName Name of the world
     * @return portals on the world
     */
    public Collection<Map.Entry<String, Position>> listPortalsOnWorld(String worldName) {
        return this.portals.getOrDefault(worldName.toLowerCase(Locale.ENGLISH), Collections.emptyMap()).entrySet();
    }

    /**
     * Looks up a portal by name, on a specific world
     * 
     * @param portalName Name of the portal to find
     * @param worldName World to look for the portal by this name
     * @return position on the world where a portal with this name exists
     */
    public Position findPortalOnWorld(String portalName, String worldName) {
        return this.portals.getOrDefault(worldName.toLowerCase(Locale.ENGLISH), Collections.emptyMap()).get(portalName);
    }

    /**
     * Looks up all the portals that have the given name, on all
     * worlds of the server. The name of the portal can match with
     * a different case, making this check more relaxed.
     * 
     * @param portalName
     * @return list of positions where a portal with this name exists
     */
    public List<Position> findPortalsRelaxed(String portalName) {
        return this.portals.values().stream()
            .flatMap(map -> map.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(portalName))
                    .map(Map.Entry::getValue))
            .collect(Collectors.toList());
    }

    private void scheduleSave() {
        if (!this.autosaveScheduled) {
            this.autosaveScheduled = true;
            this.autosaveTask.start(AUTOSAVE_INTERVAL);
        }
    }

    private void load() {
        // Clear old data
        this.portals.clear();

        // If no save file exists, do nothing
        if (!this.saveFile.exists()) {
            return;
        }

        this.autosaveScheduled = true; // suppress starting of the autosave
        try {
            BufferedReader reader = new BufferedReader(new FileReader(this.saveFile));
            try {
                String textline;
                while ((textline = reader.readLine()) != null) {
                    String[] args = StringUtil.convertArgs(textline.split(" "));
                    if (args.length == 7) {
                        String name = args[0];
                        try {
                            Position pos = new Position(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Float.parseFloat(args[5]), Float.parseFloat(args[6]));
                            pos.setX(pos.getBlockX() + 0.5);
                            pos.setZ(pos.getBlockZ() + 0.5);
                            this.storePortal(name, pos);
                        } catch (Exception ex) {
                            MyWorlds.plugin.log(Level.SEVERE, "Failed to load portal: " + name);
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                reader.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        this.autosaveScheduled = false; // further changes cause autosave
    }

    private void save() {
        // Save to a String, then write that out asynchronously
        StringBuilder saveText = new StringBuilder();
        this.portals.values().stream()
            .flatMap(map -> map.entrySet().stream())
            .forEachOrdered(e -> {
                Position pos = e.getValue();
                saveText.append("\"").append(e.getKey()).append("\" ");
                saveText.append("\"" ).append(pos.getWorldName()).append("\" ");
                saveText.append(pos.getBlockX()).append(' ');
                saveText.append(pos.getBlockY()).append(' ');
                saveText.append(pos.getBlockZ()).append(' ');
                saveText.append(pos.getYaw()).append(' ');
                saveText.append(pos.getPitch()).append('\n');
            });

        // Save it, handle errors and log them
        this.saveFuture = AsyncTextWriter.writeSafe(this.saveFile, saveText.toString());
        this.saveFuture.exceptionally(t -> {
            plugin.getLogger().log(Level.SEVERE, "Failed to save portals.txt", t);
            return null;
        });
    }
}
