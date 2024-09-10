package com.bergerkiller.bukkit.mw.playerdata;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.zip.ZipException;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.entity.CommonEntity;
import com.bergerkiller.bukkit.common.entity.type.CommonPlayer;
import com.bergerkiller.bukkit.common.nbt.CommonTagCompound;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.common.wrappers.PlayerRespawnPoint;
import com.bergerkiller.bukkit.mw.MWPlayerDataController;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;

public class PlayerDataFile {
    private static final boolean SAVE_HEAL_F = Common.evaluateMCVersion("<=", "1.8.8");

    public final String playerName;
    public final String playerUUID;
    public final WorldConfig world;
    public final File file;

    public PlayerDataFile(OfflinePlayer player, WorldConfig worldConfig) {
        this(player.getName(), player.getUniqueId().toString(), worldConfig);
    }

    public PlayerDataFile(String playerName, String playerUUID, WorldConfig worldConfig) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.world = worldConfig;
        this.file = worldConfig.getPlayerData(playerUUID);
        this.file.getParentFile().mkdirs();
    }

    static void init() {
    }

    public static File getPlayerDataFile(File playerDataFolder, UUID playerUUID) {
        return getPlayerDataFile(playerDataFolder, playerUUID.toString());
    }

    public static File getPlayerDataFile(File playerDataFolder, String playerUUID) {
        return new File(playerDataFolder, playerUUID + ".dat");
    }

    public boolean exists() {
        return file.exists();
    }

    public long lastModified() {
        return file.lastModified();
    }

    public CommonTagCompound readIfExists() {
        return tryReadIfExists(MyWorlds.plugin, file, playerName);
    }

    public static CommonTagCompound readIfExists(MyWorlds plugin, File playerDataFolder, String playerUUID) {
        return tryReadIfExists(plugin, getPlayerDataFile(playerDataFolder, playerUUID), playerUUID);
    }

    public static CommonTagCompound readIfExists(MyWorlds plugin, File playerDataFolder, UUID playerUUID) {
        return tryReadIfExists(plugin, getPlayerDataFile(playerDataFolder, playerUUID), playerUUID);
    }

    private static CommonTagCompound tryReadIfExists(MyWorlds plugin, File file, Object playerName) {
        try {
            if (file.exists()) {
                return CommonTagCompound.readFromFile(file, true);
            }
        } catch (ZipException ex) {
            plugin.getLogger().warning("Failed to read player data for " + playerName + " (ZIP-exception: file corrupted)");
        } catch (Throwable t) {
            // Return an empty data constant for now
            plugin.getLogger().log(Level.WARNING, "Failed to read player data for " + playerName, t);
        }
        return null;
    }

    public static void write(CommonTagCompound data, File playerDataFolder, UUID playerUUID) throws IOException {
        data.writeToFile(getPlayerDataFile(playerDataFolder, playerUUID), true);
    }

    public CommonTagCompound read(Player player) {
        CommonTagCompound data = this.readIfExists();
        return (data != null) ? data : createEmptyData(player);
    }

    public void write(CommonTagCompound data) throws IOException {
        data.writeToFile(file, true);
    }

    public boolean updateIfExists(DataUpdater updater) throws IOException {
        CommonTagCompound data = readIfExists();
        if (data == null) {
            return false;
        }

        updater.update(data);
        write(data);
        return true;
    }

    public void update(Player player, DataUpdater updater) throws IOException {
        CommonTagCompound data = read(player);
        updater.update(data);
        write(data);
    }

    public static PlayerDataFile mainFile(String playerName, String playerUUID) {
        return new PlayerDataFile(playerName, playerUUID, WorldConfig.getInventoryMain());
    }

    public static PlayerDataFile mainFile(OfflinePlayer player) {
        return new PlayerDataFile(player, WorldConfig.getInventoryMain());
    }

    /**
     * Creates new player data information as if the player just joined the server.
     *
     * @param player to generate information about
     * @return empty data
     */
    public static CommonTagCompound createEmptyData(Player player) {
        final Vector velocity = player.getVelocity();
        CommonTagCompound empty = new CommonTagCompound();
        CommonPlayer playerEntity = CommonEntity.get(player);
        empty.putUUID("", player.getUniqueId());
        if (SAVE_HEAL_F) {
            empty.putValue("HealF", (float) playerEntity.getMaxHealth());
        }
        empty.putValue("Health", (float) playerEntity.getMaxHealth());
        empty.putValue("HurtTime", (short) 0);
        empty.putValue("DeathTime", (short) 0);
        empty.putValue("AttackTime", (short) 0);
        empty.putListValues("Motion", velocity.getX(), velocity.getY(), velocity.getZ());
        setLocation(empty, WorldManager.getSpawnLocation(MyWorlds.getMainWorld()));
        PlayerRespawnPoint.forPlayer(player).toNBT(empty);
        return empty;
    }

    /**
     * Creates new player data information as if the player just joined the server.
     * Used for offline reading if the player data of another world was lost.
     * 
     * @param playerUUID UUID String of the Player to generate information about
     * @return empty data
     */
    public static CommonTagCompound createEmptyData(String playerUUID) {
        CommonTagCompound empty = new CommonTagCompound();
        try {
            empty.putUUID("", UUID.fromString(playerUUID));
        } catch (IllegalArgumentException ex) { /* ignore */}

        if (SAVE_HEAL_F) {
            empty.putValue("HealF", 20.0f);
        }
        empty.putValue("Health", 20.0f);
        empty.putValue("HurtTime", (short) 0);
        empty.putValue("DeathTime", (short) 0);
        empty.putValue("AttackTime", (short) 0);
        empty.putListValues("Motion", 0.0, 0.0, 0.0);
        setLocation(empty, WorldManager.getSpawnLocation(MyWorlds.getMainWorld()));
        PlayerRespawnPoint.NONE.toNBT(empty);
        return empty;
    }

    public static void setLocation(CommonTagCompound tagCompound, Location location) {
        tagCompound.putListValues("Pos", location.getX(), location.getY(), location.getZ());
        tagCompound.putListValues("Rotation", location.getYaw(), location.getPitch());
        final World world = location.getWorld();
        tagCompound.putValue("World", world.getName());
        tagCompound.putUUID("World", world.getUID());

        if (Common.evaluateMCVersion(">=","1.16")) {
            // Dimension stored as a string key
            tagCompound.putMinecraftKey("Dimension", WorldUtil.getDimensionKey(world).getName());
        } else {
            // Dimension TYPE stored as an ID. Server uses world name / uuid fields to figure out what world.
            tagCompound.putValue("Dimension", WorldUtil.getDimensionType(world).getId());
        }
    }

    /**
     * Checks whether the main world file data stores data for all worlds on the server. This is the case
     * when a profile is saved before MyWorlds multi-world inventories is enabled. or before MyWorlds is even
     * installed.
     *
     * @param mainWorldData
     * @return True if the player profile data is self-contained
     */
    public static boolean isSelfContained(CommonTagCompound mainWorldData) {
        // On modern versions it has a separate data tag to track whether all player data was shared.
        // This prevents some annoying edge-cases from tripping this code up.
        {
            CommonTagCompound myworlds = mainWorldData.get(MWPlayerDataController.DATA_TAG_ROOT, CommonTagCompound.class);
            if (myworlds != null) {
                Boolean is_self_contained = myworlds.getValue(MWPlayerDataController.DATA_TAG_IS_SELF_CONTAINED, Boolean.class);
                if (is_self_contained != null) {
                    return is_self_contained.booleanValue();
                }
            }
        }

        // Legacy stuff...
        {
            // Check whether the inventory sharing rules for the data's world specify a different world
            UUID worldUUID = mainWorldData.getUUID("World");
            UUID mw_legacy_inventory_world = mainWorldData.getUUID(MWPlayerDataController.LEGACY_DATA_TAG_LASTWORLD);
            if (worldUUID != null && mw_legacy_inventory_world != null) {
                // If different that is a strong indication that inventory data was stored split per world.
                // This is not perfect, however. And assumes the main world is also an inventory storing world.
                return worldUUID.equals(mw_legacy_inventory_world);
            }

            // Assume true
            return true;
        }
    }

    @FunctionalInterface
    public static interface DataUpdater {
        public void update(CommonTagCompound data);
    }
}
