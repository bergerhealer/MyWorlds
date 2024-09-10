package com.bergerkiller.bukkit.mw.playerdata;

import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class PlayerDataFileCollection {
    public final String playerName;
    public final String playerUUID;
    public PlayerDataFile mainWorldFile; /* Stores the name of the main world the player is on */
    public PlayerDataFile positionFile;  /* Stores the position of the player on a particular world */
    public PlayerDataFile currentFile;   /* Stores the inventory and data of the player on a particular world. Merge/split rules apply. */

    static void init() {
    }

    public PlayerDataFileCollection(OfflinePlayer player, World world) {
        this(player.getName(), player.getUniqueId().toString(), world);
    }

    public PlayerDataFileCollection(String playerName, String playerUUID, World world) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.mainWorldFile = PlayerDataFile.mainFile(playerName, playerUUID);

        setCurrentWorld(world);
    }

    /**
     * Creates a new PlayerDataFile instance for a World
     *
     * @param worldConfig WorldConfig of the world
     * @return new PlayerDataFile
     */
    public PlayerDataFile createFile(WorldConfig worldConfig) {
        return new PlayerDataFile(playerName, playerUUID, worldConfig);
    }

    /**
     * Sets the current world the player is in, switching the file locations
     *
     * @param currentWorld the player is in
     */
    public void setCurrentWorld(World currentWorld) {
        setCurrentWorld(WorldConfig.get(currentWorld));
    }

    /**
     * Sets the current world the player is in, switching the file locations
     * 
     * @param currentWorldConfig WorldConfig of the current world the player is in
     */
    public void setCurrentWorld(WorldConfig currentWorldConfig) {
        this.positionFile = createFile(currentWorldConfig);
        if (MyWorlds.useWorldInventories) {
            WorldConfig inventoryWorld = WorldConfig.get(currentWorldConfig.inventory.getSharedWorldName());
            this.currentFile = createFile(inventoryWorld);
        } else {
            this.currentFile = this.mainWorldFile;
        }
    }

    /**
     * Both position and inventory state information is saved in the same file
     * 
     * @return True if it is the same file, False if not
     */
    public boolean isSingleDataFile() {
        return positionFile.file.equals(currentFile.file);
    }

    /**
     * The inventory information is stored in the same file as the main world information.
     * If this is the case, the world the player is on does not have to be updated separately.
     * 
     * @return True if the inventory and main world are saved in the same file
     */
    public boolean isMainWorld() {
        return mainWorldFile.file.equals(currentFile.file);
    }

    /* Debug */
    public void log(String title) {
        /*
        System.out.println("Files used " + title + " for player " + player.getName() + " world " + player.getWorld().getName() + ":");
        System.out.println("  mainWorldFile: " + mainWorldFile.file);
        System.out.println("  positionFile: " + positionFile.file);
        System.out.println("  inventoryFile: " + currentFile.file);
        */
    }
}