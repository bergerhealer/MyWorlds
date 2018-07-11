package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipException;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.bergerkiller.bukkit.common.controller.PlayerDataController;
import com.bergerkiller.bukkit.common.entity.CommonEntity;
import com.bergerkiller.bukkit.common.entity.type.CommonHumanEntity;
import com.bergerkiller.bukkit.common.entity.type.CommonPlayer;
import com.bergerkiller.bukkit.common.nbt.CommonTagCompound;
import com.bergerkiller.bukkit.common.nbt.CommonTagList;
import com.bergerkiller.bukkit.common.protocol.PacketType;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.NBTUtil;
import com.bergerkiller.bukkit.common.utils.PacketUtil;
import com.bergerkiller.bukkit.common.utils.PlayerUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.generated.net.minecraft.server.EntityLivingHandle;
import com.bergerkiller.generated.net.minecraft.server.MobEffectHandle;
import com.bergerkiller.generated.net.minecraft.server.MobEffectListHandle;
import com.bergerkiller.reflection.net.minecraft.server.NMSEntityHuman;
import com.bergerkiller.reflection.net.minecraft.server.NMSEntityLiving;
import com.bergerkiller.reflection.net.minecraft.server.NMSMobEffect;

public class MWPlayerDataController extends PlayerDataController {
    public static final String DATA_TAG_LASTPOS = "MyWorlds.playerPos";
    public static final String DATA_TAG_LASTROT = "MyWorlds.playerRot";

    private static void setLocation(CommonTagCompound tagCompound, Location location) {
        tagCompound.putListValues("Pos", location.getX(), location.getY(), location.getZ());
        tagCompound.putListValues("Rotation", location.getYaw(), location.getPitch());
        final World world = location.getWorld();
        tagCompound.putValue("World", world.getName());
        tagCompound.putUUID("World", world.getUID());
        tagCompound.putValue("Dimension", WorldUtil.getDimension(world));
    }

    /**
     * Creates new human data information as if the human just joined the server.
     * 
     * @param human to generate information about
     * @return empty data
     */
    public static CommonTagCompound createEmptyData(HumanEntity human) {
        final Vector velocity = human.getVelocity();
        CommonTagCompound empty = new CommonTagCompound();
        CommonHumanEntity<?> humanEntity = CommonEntity.get(human);
        empty.putUUID("", human.getUniqueId());
        empty.putValue("Health", (short) humanEntity.getMaxHealth());
        empty.putValue("Max Health", (float) humanEntity.getMaxHealth()); // since 1.6.1 health is a float
        empty.putValue("HurtTime", (short) 0);
        empty.putValue("DeathTime", (short) 0);
        empty.putValue("AttackTime", (short) 0);
        empty.putListValues("Motion", velocity.getX(), velocity.getY(), velocity.getZ());
        setLocation(empty, WorldManager.getSpawnLocation(MyWorlds.getMainWorld()));
        empty.putBlockLocation("Spawn", humanEntity.getSpawnPoint());
        return empty;
    }

    /**
     * Attempts to read the last known Player position on a specific World
     * 
     * @param player to get the last position for
     * @param world to get the last position for
     * @return Last known Location, or null if not found/stored
     */
    public static Location readLastLocation(Player player, World world) {
        PlayerFile posFile = new PlayerFile(player, world.getName());
        if (!posFile.exists()) {
            return null;
        }

        CommonTagCompound data = posFile.read();
        CommonTagList posInfo = data.getValue(DATA_TAG_LASTPOS, CommonTagList.class);
        if (posInfo != null && posInfo.size() == 3) {
            // Apply position
            Location location = new Location(world, posInfo.getValue(0, 0.0), posInfo.getValue(1, 0.0), posInfo.getValue(2, 0.0));
            CommonTagList rotInfo = data.getValue(DATA_TAG_LASTROT, CommonTagList.class);
            if (rotInfo != null && rotInfo.size() == 2) {
                location.setYaw(rotInfo.getValue(0, 0.0f));
                location.setPitch(rotInfo.getValue(1, 0.0f));
            }
            return location;
        }
        return null;
    }

    private static void clearEffects(HumanEntity human) {
        // Clear mob effects
        EntityLivingHandle livingHandle = EntityLivingHandle.fromBukkit(human);

        Map<MobEffectListHandle, MobEffectHandle> effects = livingHandle.getMobEffects();
        if (human instanceof Player) {
            // Send mob effect removal messages
            Player player = (Player) human;
            for (MobEffectListHandle effect : effects.keySet()) {
                PacketUtil.sendPacket(player, PacketType.OUT_ENTITY_EFFECT_REMOVE.newInstance(player.getEntityId(), effect));
            }
        }
        effects.clear();
        // Clear attributes
        NBTUtil.resetAttributes(human);
        NMSEntityLiving.initAttributes.invoke(livingHandle.getRaw());
    }

    /**
     * Handles post loading of an Entity
     * 
     * @param human that got loaded
     */
    private static void postLoad(HumanEntity human) {
        if (WorldConfig.get(human.getWorld()).clearInventory) {
            human.getInventory().clear();
            clearEffects(human);
        }
    }

    /**
     * Applies the player states in the world to the player specified
     * 
     * @param player to set the states for
     */
    public static void refreshState(Player player) {
        if (!MyWorlds.useWorldInventories) {
            // If not enabled, only do the post-load logic
            postLoad(player);
            return;
        }
        try {
            final PlayerFileCollection files = new PlayerFileCollection(player);
            final CommonTagCompound playerData = files.currentFile.read();

            files.log("refreshing state");

            CommonPlayer commonPlayer = CommonEntity.get(player);
            EntityLivingHandle livingPlayer = EntityLivingHandle.fromBukkit(player);
            Object playerHandle = commonPlayer.getHandle();

            // First, clear previous player information when loading involves adding new elements
            clearEffects(player);

            // Refresh attributes
            if (playerData.containsKey("Attributes")) {
                NBTUtil.loadAttributes(player, playerData.get("Attributes", CommonTagList.class));
            }

            // Load the data
            NBTUtil.loadInventory(player.getInventory(), playerData.createList("Inventory"));
            commonPlayer.write(NMSEntityHuman.exp, playerData.getValue("XpP", 0.0f));
            commonPlayer.write(NMSEntityHuman.expLevel, playerData.getValue("XpLevel", 0));
            commonPlayer.write(NMSEntityHuman.expTotal, playerData.getValue("XpTotal", 0));

            if (playerData.containsKey("HealF")) {
                // Legacy stuff
                commonPlayer.setHealth(playerData.getValue("HealF", float.class));
            } else {
                commonPlayer.setHealth(playerData.getValue("Health", commonPlayer.getMaxHealth()));
            }

            commonPlayer.setSpawnPoint(playerData.getBlockLocation("Spawn"));
            commonPlayer.write(NMSEntityHuman.spawnForced, playerData.getValue("SpawnForced", false));
            NBTUtil.loadFoodMetaData(NMSEntityHuman.foodData.get(playerHandle), playerData);
            NBTUtil.loadInventory(player.getEnderChest(), playerData.createList("EnderItems"));

            if (playerData.containsKey("playerGameType")) {
                player.setGameMode(GameMode.getByValue(playerData.getValue("playerGameType", 1)));
            }

            // data.getValue("Bukkit.MaxHealth", (float) commonPlayer.getMaxHealth());

            // Load Mob Effects
            Map<MobEffectListHandle, MobEffectHandle> effects = livingPlayer.getMobEffects();
            if (playerData.containsKey("ActiveEffects")) {
                CommonTagList taglist = playerData.createList("ActiveEffects");
                for (int i = 0; i < taglist.size(); ++i) {
                    MobEffectHandle mobEffect = NBTUtil.loadMobEffect((CommonTagCompound) taglist.get(i));
                    effects.put(mobEffect.getEffectList(), mobEffect);
                }
            }
            livingPlayer.setUpdateEffects(true);

            // Send add messages for all (new) effects
            for (Object effect : effects.values()) {
                PacketUtil.sendPacket(player, PacketType.OUT_ENTITY_EFFECT_ADD.newInstance(player.getEntityId(), effect));
            }

            // Perform post loading
            postLoad(player);
        } catch (Exception exception) {
            Bukkit.getLogger().warning("Failed to load player data for " + player.getName());
            exception.printStackTrace();
        }
    }

    private static void removeBedSpawnPointIfDisabled(CommonTagCompound playerData) {
        String bedSpawnWorld = playerData.getValue("SpawnWorld", "");
        if (bedSpawnWorld != null && !bedSpawnWorld.isEmpty()) {
            WorldConfig config = WorldConfig.getIfExists(bedSpawnWorld);
            if (config != null && !config.bedRespawnEnabled) {
                playerData.removeValue("SpawnWorld");
                playerData.removeValue("SpawnForced");
                playerData.removeValue("SpawnX");
                playerData.removeValue("SpawnY");
                playerData.removeValue("SpawnZ");
            }
        }
    }

    /**
     * Fired when a player respawns and all it's settings will be wiped.
     * The player contains all information right before respawning.
     * All data that would be wiped should be written has being wiped.
     * This involves a manual save.
     * 
     * @param player that respawned
     * @param respawnLocation where the player respawns at
     */
    public void onRespawnSave(Player player, final Location respawnLocation) {
        try {
            final PlayerFileCollection files = new PlayerFileCollection(player);
            final CommonTagCompound savedData = createEmptyData(player);
            CommonTagCompound playerSavedData = NBTUtil.saveEntity(player, null);

            files.log("saving data after respawn");

            // We store this entire Bukkit tag + experience information!
            CommonTagCompound bukkitTag = playerSavedData.get("bukkit", CommonTagCompound.class);
            if (bukkitTag != null) {
                // But, we do need to wipe information as specified
                if (bukkitTag.getValue("keepLevel", false)) {
                    // Preserve experience
                    bukkitTag.putValue("newTotalExp", playerSavedData.getValue("XpTotal", 0));
                    bukkitTag.putValue("newLevel", playerSavedData.getValue("XpLevel", 0));
                    savedData.putValue("XpP", playerSavedData.getValue("XpP", 0.0f));
                }
                // Store experience (if not preserved, uses newTotal/newLevel) and the tag
                savedData.putValue("XpTotal", bukkitTag.getValue("newTotalExp", 0));
                savedData.putValue("XpLevel", bukkitTag.getValue("newLevel", 0));
                savedData.put("bukkit", bukkitTag);
            }

            // Ender inventory should not end up wiped!
            CommonTagList enderItems = playerSavedData.get("EnderItems", CommonTagList.class);
            if (enderItems != null) {
                savedData.put("EnderItems", enderItems);
            }

            // Disable bed spawn if not enabled for that world
            removeBedSpawnPointIfDisabled(savedData);

            // Now, go ahead and save this data
            files.currentFile.write(savedData);

            // Finally, we need to update where the player is at right now
            // To do so, we will write a new main world where the player is meant to be
            // This operation is a bit optional at this point, but it avoids possible issues in case of crashes
            // This is only needed if a main player data file doesn't exist
            // (this should in theory never happen either...player is not joining)
            if (files.mainWorldFile.exists()) {
                files.mainWorldFile.update(new DataUpdater() {
                    @Override
                    public void update(CommonTagCompound data) {
                        data.putUUID("World", respawnLocation.getWorld().getUID());
                    }
                });
            }
        } catch (Exception exception) {
            Bukkit.getLogger().warning("Failed to save player respawned data for " + player.getName());
            exception.printStackTrace();
        }
    }

    @Override
    public CommonTagCompound onLoad(HumanEntity human) {
        try {
            final PlayerFileCollection files = new PlayerFileCollection(human);

            CommonTagCompound mainWorldData = null;
            boolean hasPlayedBefore = false;

            // If a main world player data file exists, then the player has been on the server before
            hasPlayedBefore = files.mainWorldFile.exists();

            // Find out where to find the save file
            // No need to check for this if not using world inventories - it is always the main file then
            if (MyWorlds.useWorldInventories && hasPlayedBefore && !MyWorlds.forceMainWorldSpawn) {
                try {
                    // Allow switching worlds and positions
                    mainWorldData = files.mainWorldFile.read();
                    World world = Bukkit.getWorld(mainWorldData.getUUID("World"));
                    if (world != null) {
                        // Switch to the save file of the loaded world
                        files.setCurrentWorld(world);
                    }
                } catch (Throwable t) {
                    // Stick with the current world for now.
                }
            }

            files.log("loading data");

            // Load player data from the right world
            // Optimization: if main world, we can re-use the read earlier
            CommonTagCompound playerData;
            if (mainWorldData != null && files.isMainWorld()) {
                playerData = mainWorldData;
            } else {
                playerData = files.currentFile.read();
            }

            // Disable bed spawn if not enabled for that world
            removeBedSpawnPointIfDisabled(playerData);

            // When main world spawning is forced, reset location to there
            if (!hasPlayedBefore || MyWorlds.forceMainWorldSpawn) {
                setLocation(playerData, WorldManager.getSpawnLocation(MyWorlds.getMainWorld()));
            }

            // Minecraft bugfix here: Clear effects BEFORE loading the data
            // This resolves issues with effects staying behind
            clearEffects(human);

            // Load the entity using the player data compound
            NBTUtil.loadEntity(human, playerData);
            if (human instanceof Player) {
                // Bukkit bug: entityplayer.e(tag) -> b(tag) -> craft.readExtraData(tag) which instantly sets it
                // Make sure the player is marked as being new
                PlayerUtil.setHasPlayedBefore((Player) human, hasPlayedBefore);
                if (hasPlayedBefore) {
                    // As specified in the WorldNBTStorage implementation, set this
                    PlayerUtil.setFirstPlayed((Player) human, files.mainWorldFile.lastModified());
                }
            }
            postLoad(human);

            // When set as flying, there appears to be a problem or bug where this is reset
            // This causes the player to come falling down upon (re-)join, which is really annoying
            // For now this is fixed by explicitly calling setFlying one tick later
            if (human instanceof Player && playerData.containsKey("abilities")) {
                CommonTagCompound abilities = (CommonTagCompound) playerData.get("abilities");
                if (abilities.getValue("flying", false)) {
                    final Player player = (Player) human;
                    CommonUtil.nextTick(new Runnable() {
                        @Override
                        public void run() {
                            if (player.isOnline()) {
                                try {
                                    player.setFlying(true);
                                } catch (IllegalArgumentException ex) {}
                            }
                        }
                    });
                }
            }

            return playerData;
        } catch (Exception exception) {
            Bukkit.getLogger().warning("Failed to load player data for " + human.getName());
            exception.printStackTrace();
            return super.onLoad(human);
        }
    }

    @Override
    public void onSave(HumanEntity human) {
        try {
            final PlayerFileCollection files = new PlayerFileCollection(human);
            final CommonTagCompound savedData = NBTUtil.saveEntity(human, null);

            // Disable bed spawn if not enabled for that world
            removeBedSpawnPointIfDisabled(savedData);

            files.log("saving data");

            final Location loc = human.getLocation();
            if (files.isSingleDataFile()) {
                // Append the Last Pos/Rot to the data
                savedData.putListValues(DATA_TAG_LASTPOS, loc.getX(), loc.getY(), loc.getZ());
                savedData.putListValues(DATA_TAG_LASTROT, loc.getYaw(), loc.getPitch());
            } else {
                // Append original last position (if available) to the data
                if (files.currentFile.exists()) {
                    CommonTagCompound data = files.currentFile.read();
                    if (data.containsKey(DATA_TAG_LASTPOS)) {
                        savedData.put(DATA_TAG_LASTPOS, data.get(DATA_TAG_LASTPOS));
                    }
                    if (data.containsKey(DATA_TAG_LASTROT)) {
                        savedData.put(DATA_TAG_LASTROT, data.get(DATA_TAG_LASTROT));
                    }
                }

                // Write the Last Pos/Rot to the official world file instead
                files.positionFile.update(new DataUpdater() {
                    @Override
                    public void update(CommonTagCompound data) {
                        data.putListValues(DATA_TAG_LASTPOS, loc.getX(), loc.getY(), loc.getZ());
                        data.putListValues(DATA_TAG_LASTROT, loc.getYaw(), loc.getPitch());
                    }
                });
            }

            // Save data to the destination file
            files.currentFile.write(savedData);

            // Write the current world name of the player to the save file of the main world
            if (!files.isMainWorld()) {
                // Update the world in the main file
                files.mainWorldFile.update(new DataUpdater() {
                    @Override
                    public void update(CommonTagCompound data) {
                        data.put("Pos", savedData.get("Pos"));
                        data.put("Rotation", savedData.get("Rotation"));
                        data.putUUID("World", files.human.getWorld().getUID());
                    }
                });
            }
        } catch (Exception exception) {
            Bukkit.getLogger().warning("Failed to save player data for " + human.getName());
            exception.printStackTrace();
        }
    }

    public interface DataUpdater {
        public void update(CommonTagCompound data);
    }

    public static class PlayerFile {
        public final HumanEntity human;
        public final File file;

        public PlayerFile(HumanEntity human, String worldName) {
            this(human, WorldConfig.get(worldName));
        }

        public PlayerFile(HumanEntity human, WorldConfig worldConfig) {
            this.human = human;
            this.file = worldConfig.getPlayerData(human);
            this.file.getParentFile().mkdirs();
        }

        public boolean exists() {
            return file.exists();
        }

        public long lastModified() {
            return file.lastModified();
        }

        public CommonTagCompound read() {
            try {
                if (file.exists()) {
                    return CommonTagCompound.readFromFile(file, true);
                }
            } catch (ZipException ex) {
                Bukkit.getLogger().warning("Failed to read player data for " + human.getName() + " (ZIP-exception: file corrupted)");
            } catch (Throwable t) {
                // Return an empty data constant for now
                Bukkit.getLogger().warning("Failed to read player data for " + human.getName());
                t.printStackTrace();
            }
            return createEmptyData(human);
        }

        public void write(CommonTagCompound data) throws IOException {
            data.writeToFile(file, true);
        }

        public void update(DataUpdater updater) throws IOException {
            CommonTagCompound data = read();
            updater.update(data);
            write(data);
        }
    }

    public static class PlayerFileCollection {
        public final HumanEntity human;
        public PlayerFile mainWorldFile; /* Stores the name of the main world the player is on */
        public PlayerFile positionFile;  /* Stores the position of the player on a particular world */
        public PlayerFile currentFile;   /* Stores the inventory and data of the player on a particular world. Merge/split rules apply. */

        public PlayerFileCollection(HumanEntity human) {
            this.human = human;
            this.mainWorldFile = new PlayerFile(human, WorldConfig.getMain());
            setCurrentWorld(human.getWorld());
        }

        /**
         * Sets the current world the human is in, switching the file locations
         * 
         * @param currentWorld the human is in
         */
        public void setCurrentWorld(World currentWorld) {
            WorldConfig currentWorldConfig = WorldConfig.get(currentWorld);
            this.positionFile = new PlayerFile(human, currentWorldConfig);
            if (MyWorlds.useWorldInventories) {
                this.currentFile = new PlayerFile(human, currentWorldConfig.inventory.getSharedWorldName());
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
            System.out.println("Files used " + title + " for player " + human.getName() + " world " + human.getWorld().getName() + ":");
            System.out.println("  mainWorldFile: " + mainWorldFile.file);
            System.out.println("  positionFile: " + positionFile.file);
            System.out.println("  inventoryFile: " + currentFile.file);
            */
        }
    }
}
