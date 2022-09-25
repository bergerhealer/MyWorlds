package com.bergerkiller.bukkit.mw.playerdata;

import java.io.File;
import java.io.IOException;
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
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;

public class PlayerDataFile {
    private static final boolean SAVE_HEAL_F = Common.evaluateMCVersion("<=", "1.8.8");

    public final OfflinePlayer player;
    public final WorldConfig world;
    public final File file;

    public PlayerDataFile(OfflinePlayer player, WorldConfig worldConfig) {
        this.player = player;
        this.world = worldConfig;
        this.file = worldConfig.getPlayerData(player);
        this.file.getParentFile().mkdirs();
    }

    public boolean exists() {
        return file.exists();
    }

    public long lastModified() {
        return file.lastModified();
    }

    public CommonTagCompound readIfExists() {
        try {
            if (file.exists()) {
                return CommonTagCompound.readFromFile(file, true);
            }
        } catch (ZipException ex) {
            MyWorlds.plugin.getLogger().warning("Failed to read player data for " + player.getName() + " (ZIP-exception: file corrupted)");
        } catch (Throwable t) {
            // Return an empty data constant for now
            MyWorlds.plugin.getLogger().log(Level.WARNING, "Failed to read player data for " + player.getName(), t);
        }
        return null;
    }

    public CommonTagCompound read(Player player) {
        CommonTagCompound data = this.readIfExists();
        return (data != null) ? data : createEmptyData(player);
    }

    public void write(CommonTagCompound data) throws IOException {
        data.writeToFile(file, true);
    }

    public void update(Player player, DataUpdater updater) throws IOException {
        CommonTagCompound data = read(player);
        updater.update(data);
        write(data);
    }

    public static PlayerDataFile mainFile(OfflinePlayer player) {
        return new PlayerDataFile(player, MyWorlds.storeInventoryInMainWorld ?
                WorldConfig.getMain() : WorldConfig.getVanillaMain());
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

    public static void setLocation(CommonTagCompound tagCompound, Location location) {
        tagCompound.putListValues("Pos", location.getX(), location.getY(), location.getZ());
        tagCompound.putListValues("Rotation", location.getYaw(), location.getPitch());
        final World world = location.getWorld();
        tagCompound.putValue("World", world.getName());
        tagCompound.putUUID("World", world.getUID());
        tagCompound.putValue("Dimension", WorldUtil.getDimensionType(world).getId());
    }

    @FunctionalInterface
    public static interface DataUpdater {
        public void update(CommonTagCompound data);
    }
}
