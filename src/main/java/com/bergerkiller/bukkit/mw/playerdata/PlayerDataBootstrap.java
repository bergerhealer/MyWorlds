package com.bergerkiller.bukkit.mw.playerdata;

/**
 * Just got ensuring all classes used for saving/loading player data
 * are loaded up-front
 */
public class PlayerDataBootstrap {
    public static void init() {
        LastPlayerPositionList.init();
        PlayerDataFile.init();
        PlayerDataFileCollection.init();
        PlayerDataMigrator.init();
    }
}
