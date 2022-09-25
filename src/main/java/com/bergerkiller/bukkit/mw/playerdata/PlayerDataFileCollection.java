package com.bergerkiller.bukkit.mw.playerdata;

import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class PlayerDataFileCollection {
    public final OfflinePlayer player;
    public PlayerDataFile mainWorldFile; /* Stores the name of the main world the player is on */
    public PlayerDataFile positionFile;  /* Stores the position of the player on a particular world */
    public PlayerDataFile currentFile;   /* Stores the inventory and data of the player on a particular world. Merge/split rules apply. */

    public PlayerDataFileCollection(OfflinePlayer player, World world) {
        this.player = player;
        this.mainWorldFile = PlayerDataFile.mainFile(player);

        setCurrentWorld(world);
    }

    /**
     * Sets the current world the player is in, switching the file locations
     * 
     * @param currentWorld the player is in
     */
    public void setCurrentWorld(World currentWorld) {
        WorldConfig currentWorldConfig = WorldConfig.get(currentWorld);
        this.positionFile = new PlayerDataFile(player, currentWorldConfig);
        if (MyWorlds.useWorldInventories) {
            WorldConfig inventoryWorld = WorldConfig.get(currentWorldConfig.inventory.getSharedWorldName());
            this.currentFile = new PlayerDataFile(player, inventoryWorld);
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