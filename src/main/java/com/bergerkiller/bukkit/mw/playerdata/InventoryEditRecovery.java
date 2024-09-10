package com.bergerkiller.bukkit.mw.playerdata;

import com.bergerkiller.bukkit.common.nbt.CommonTagCompound;
import com.bergerkiller.bukkit.mw.MWPlayerDataController;
import com.bergerkiller.bukkit.mw.WorldConfig;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Optional;

/**
 * Utilities and logic to recover from an inventory editing plugin altering the contents of the
 * vanilla main world player data file.
 */
public class InventoryEditRecovery {
    /**
     * This data tag is set with the world name if the current file was written out as a result
     * of an inventory editing plugin. In that case, the original inventory needs to be
     * recovered from the main world. The world name specified here is where updated inventory
     * data should be written to.
     */
    public static final String DATA_TAG_INV_EDIT_WORLD = "inventoryEditWorld";
    /**
     * This data tag stores the original contents of a player data file, when player data is
     * read by an inventory editing plugin like OpenInv. This copy tag is found again the
     * next time the file is read, recovering the original inventory of that world.<br>
     * <br>
     * If the player current world is already the same world the player data file is stored at,
     * then it does not do this.<br>
     * <br>
     * When the player later joins again, MyWorlds performs a recovery procedure writing the
     * data to the correct world.
     */
    public static final String DATA_TAG_INV_EDIT_RECOVERY = "inventoryEditRecovery";

    /**
     * If loaded by an inventory editing plugin, we must assume this plugin is going to write
     * this data to the vanilla main world. If this vanilla main world isn't the world the
     * read data is for (current world of player), back up the original contents of this file.
     * This way we can recover the original inventory of this world.
     *
     * If the vanilla world is also the MyWorlds main world (usually the case), then we pass this
     * information back to the inventory editing plugin as well. Otherwise, we update the main
     * inventory world file ourselves to include this data.
     *
     * @param files PlayerData files of Player
     * @param mainWorldData Decoded Main World Data of the MyWorlds Main World PlayerFile
     * @param playerData Current player data that will be received by the inventory editing plugin
     */
    public static void writeInventoryRecoveryData(
            final PlayerDataFileCollection files,
            final CommonTagCompound mainWorldData,
            final CommonTagCompound playerData
    ) throws IOException {
        // Can't do any of this if the player never joined at least once to begin with...
        if (mainWorldData == null) {
            return;
        }

        // Don't write recovery data if the current file IS the vanilla main world. Then the changes by the
        // inventory editing plugin will have no effect
        if (files.currentFile.world == WorldConfig.getVanillaMain()) {
            return;
        }

        // If another recovery is already ongoing, do not write out another recovery as this
        // would overwrite the original data.
        if (readInventoryRecoveryData(mainWorldData).isPresent()) {
            return;
        }

        // Store the world the inventory recovery data is for
        writeEditedInventoryWorld(playerData, files.currentFile.world.worldname);

        // Recover the original data of this world
        if (WorldConfig.getVanillaMain() == WorldConfig.getInventoryMain()) {
            // Same main, include it as data we send back to the inventory editing plugin
            if (playerData == mainWorldData) {
                // Note: I don't think this can ever happen. But if it did, it would be disastrous
                // to create a circular reference. Better safe than sorry!
                playerData.createCompound(MWPlayerDataController.DATA_TAG_ROOT)
                        .put(DATA_TAG_INV_EDIT_RECOVERY, mainWorldData.clone());
            } else {
                playerData.createCompound(MWPlayerDataController.DATA_TAG_ROOT)
                        .put(DATA_TAG_INV_EDIT_RECOVERY, mainWorldData);
            }
        } else {
            // Different main, update the main world ourselves
            // Read the original vanilla world player file data
            PlayerDataFile vanillaDataFile = files.createFile(WorldConfig.getVanillaMain());
            CommonTagCompound vanillaWorldDataRead = vanillaDataFile.readIfExists();
            if (vanillaWorldDataRead != null) {
                mainWorldData.createCompound(MWPlayerDataController.DATA_TAG_ROOT)
                        .put(DATA_TAG_INV_EDIT_RECOVERY, vanillaWorldDataRead);
                files.mainWorldFile.write(mainWorldData);
            }
        }
    }

    /**
     * Sees if the main world contains an inventory recovery segment, and if it does, recovers the inventories
     * bringing data back into correct alignment.
     *
     * @param player Player
     * @param mainWorldPlayerData Previously read main world data
     * @return True if data was changed and a reload is required, False if no recovery of any sort occurred
     * @throws IOException
     */
    public static boolean recoverInventoryData(Player player, CommonTagCompound mainWorldPlayerData) throws IOException {
        // See if recovery data exists in the main world player data
        Optional<CommonTagCompound> vanillaRecoveryData = readInventoryRecoveryData(mainWorldPlayerData);
        if (!vanillaRecoveryData.isPresent()) {
            return false;
        }

        // Verify that the vanilla world this is for was indeed inventory edited
        PlayerDataFile vanillaProfile = new PlayerDataFile(player, WorldConfig.getVanillaMain());
        CommonTagCompound vanillaEditedData = vanillaProfile.readIfExists();
        Optional<String> editedWorldName = readEditedInventoryWorld(vanillaEditedData);
        if (!editedWorldName.isPresent()) {
            // Avoid trying again next time. No need to reload.
            clearInventoryRecoveryData(player, mainWorldPlayerData);
            return false;
        }

        // Recover the original data of the vanilla main world
        // This also gets rid of the recovery metadata it the vanilla main is the same as MyWorlds main world
        {
            PlayerDataFile vanillaPlayerDataFile = new PlayerDataFile(player, WorldConfig.getVanillaMain());
            vanillaPlayerDataFile.write(vanillaRecoveryData.get());
        }

        // Apply the inventory changes to the world that was edited.
        // Find the edited world's player data file. If it (still) exists, update
        // the things an inventory editing plugin is likely to be changing. Nothing more.
        {
            WorldConfig editedWorldConfig = WorldConfig.getIfExists(editedWorldName.get());
            if (editedWorldConfig != null) {
                PlayerDataFile editedWorldPlayerDataFile = new PlayerDataFile(player, editedWorldConfig);
                editedWorldPlayerDataFile.updateIfExists(data -> {
                    for (String tag : new String[] {
                            MWPlayerDataController.VANILLA_INVENTORY_TAG,
                            MWPlayerDataController.VANILLA_ENDER_CHEST_TAG
                    }) {
                        data.put(tag,
                                vanillaEditedData.createList(tag));
                    }
                });
            }
        }

        // Strip the MyWorlds main world data of the recovery metadata, if the vanilla main isn't this
        // same file. This avoids recovery being attempted next login.
        if (WorldConfig.getVanillaMain() != WorldConfig.getInventoryMain()) {
            clearInventoryRecoveryData(player, mainWorldPlayerData);
        }

        return true; // We made changes!
    }

    /**
     * Gets rid of the recovery data in the main world. Even if we don't get to this point,
     * the previous recovery already got rid of the world name.
     * Which effectively also makes it exit this mode.
     * As such this is more of a data saving / performance tweak.
     *
     * @param player Player
     * @param mainWorldPlayerData Previously read MyWorlds main world player data
     * @throws IOException
     */
    private static void clearInventoryRecoveryData(Player player, CommonTagCompound mainWorldPlayerData) throws IOException {
        mainWorldPlayerData.createCompound(MWPlayerDataController.DATA_TAG_ROOT)
                .remove(DATA_TAG_INV_EDIT_RECOVERY);
        PlayerDataFile myworldsMainPlayerDataFile = new PlayerDataFile(player, WorldConfig.getInventoryMain());
        myworldsMainPlayerDataFile.write(mainWorldPlayerData);
    }

    public static Optional<CommonTagCompound> readInventoryRecoveryData(CommonTagCompound mainWorldPlayerData) {
        if (mainWorldPlayerData != null) {
            CommonTagCompound myworldsData = mainWorldPlayerData.get(MWPlayerDataController.DATA_TAG_ROOT, CommonTagCompound.class);
            if (myworldsData != null) {
                CommonTagCompound inventoryData = myworldsData.get(DATA_TAG_INV_EDIT_RECOVERY, CommonTagCompound.class);
                if (inventoryData != null) {
                    return Optional.of(inventoryData);
                }
            }
        }

        return Optional.empty();
    }

    public static void writeEditedInventoryWorld(CommonTagCompound playerData, String worldName) {
        playerData.createCompound(MWPlayerDataController.DATA_TAG_ROOT)
                .putValue(DATA_TAG_INV_EDIT_WORLD, worldName);
    }

    public static void clearEditedInventoryWorld(CommonTagCompound playerData) {
        if (readEditedInventoryWorld(playerData).isPresent()) {
            playerData.createCompound(MWPlayerDataController.DATA_TAG_ROOT)
                    .remove(DATA_TAG_INV_EDIT_WORLD);
        }
    }

    public static Optional<String> readEditedInventoryWorld(CommonTagCompound playerData) {
        if (playerData == null) {
            return Optional.empty();
        }

        CommonTagCompound myworldsData = playerData.get(MWPlayerDataController.DATA_TAG_ROOT, CommonTagCompound.class);
        if (myworldsData == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(myworldsData.getValue(DATA_TAG_INV_EDIT_WORLD, String.class, null));
    }
}
