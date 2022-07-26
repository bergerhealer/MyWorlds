package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.zip.ZipException;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.controller.PlayerDataController;
import com.bergerkiller.bukkit.common.entity.CommonEntity;
import com.bergerkiller.bukkit.common.entity.type.CommonLivingEntity;
import com.bergerkiller.bukkit.common.entity.type.CommonPlayer;
import com.bergerkiller.bukkit.common.nbt.CommonTagCompound;
import com.bergerkiller.bukkit.common.nbt.CommonTagList;
import com.bergerkiller.bukkit.common.protocol.PacketType;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.NBTUtil;
import com.bergerkiller.bukkit.common.utils.PacketUtil;
import com.bergerkiller.bukkit.common.utils.PlayerUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.common.wrappers.PlayerRespawnPoint;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutEntityEquipmentHandle;
import com.bergerkiller.generated.net.minecraft.server.level.EntityPlayerHandle;
import com.bergerkiller.generated.net.minecraft.world.effect.MobEffectHandle;
import com.bergerkiller.generated.net.minecraft.world.effect.MobEffectListHandle;
import com.bergerkiller.generated.net.minecraft.world.entity.EntityLivingHandle;

public class MWPlayerDataController extends PlayerDataController {
    public static final String DATA_TAG_LASTWORLD = "MyWorlds.playerWorld";
    public static final String DATA_TAG_LASTPOS = "MyWorlds.playerPos";
    public static final String DATA_TAG_LASTROT = "MyWorlds.playerRot";

    private static final boolean SAVE_HEAL_F = Common.evaluateMCVersion("<=", "1.8.8");

    private static final Map<Player, World> worldToSaveTo = new IdentityHashMap<>();

    /**
     * Saves a player to disk. Makes sure to remember the world the player was on
     * to prevent data being saved for the wrong world.
     *
     * @param player Player
     * @param world The world the player is/was on
     */
    public static void savePlayer(Player player, World world) {
        if (world == null) {
            CommonUtil.savePlayer(player);
            return;
        }

        synchronized (worldToSaveTo) {
            worldToSaveTo.put(player, world);
        }
        CommonUtil.savePlayer(player);
        synchronized (worldToSaveTo) {
            worldToSaveTo.remove(player);
        }
    }

    private static void setLocation(CommonTagCompound tagCompound, Location location) {
        tagCompound.putListValues("Pos", location.getX(), location.getY(), location.getZ());
        tagCompound.putListValues("Rotation", location.getYaw(), location.getPitch());
        final World world = location.getWorld();
        tagCompound.putValue("World", world.getName());
        tagCompound.putUUID("World", world.getUID());
        tagCompound.putValue("Dimension", WorldUtil.getDimensionType(world).getId());
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
     * Attempts to read the bed spawn position on a specific World
     * 
     * @param player to get the bed spawn location for
     * @param world to get the bed spawn location for
     * @return Bed spawn location, or NONE if not set / stored
     */
    public static PlayerRespawnPoint readRespawnPoint(Player player, World world) {
        PlayerFile posFile = new PlayerFile(player, world.getName());
        if (!posFile.exists()) {
            return PlayerRespawnPoint.NONE;
        }

        CommonTagCompound data = posFile.read(player);
        PlayerRespawnPoint respawn = PlayerRespawnPoint.fromNBT(data);
        return isValidRespawnPoint(world, respawn) ? respawn : PlayerRespawnPoint.NONE;
    }

    /**
     * Verifies that a respawn point is a valid respawn point according to inventory-sharing
     * configurations. Avoids a respawn at another world of which transferring the respawn
     * point from is impossible.
     *
     * @param world
     * @param respawn
     * @return True if valid, False if invalid
     */
    public static boolean isValidRespawnPoint(World world, PlayerRespawnPoint respawn) {
        if (MyWorlds.useWorldInventories && !respawn.isNone()) {
            if (!WorldConfig.get(world).inventory.contains(respawn.getWorld())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tries to find the world an offline player was last on.
     * Returns the configured main spawn/join world if no data is available,
     * or the original world isn't loaded anymore.
     *
     * @param player
     * @return Last world this player was on
     */
    public static World findPlayerWorld(OfflinePlayer player) {
        // Try to read this information
        PlayerFile mainWorldFile = new PlayerFile(player, MyWorlds.storeInventoryInMainWorld ?
                WorldConfig.getMain() : WorldConfig.getVanillaMain());
        CommonTagCompound mainWorldData = mainWorldFile.readIfExists(player);
        if (mainWorldData != null) {
            World world = Bukkit.getWorld(mainWorldData.getUUID("World"));
            if (world != null) {
                return world;
            }
        }

        // Return the main world configured
        return MyWorlds.getMainWorld();
    }

    /**
     * Checks the main world where inventory data is saved for a player and
     * reads what world in the world group of inventory state the player
     * was last on. For this world, the last-known position is returned.
     * 
     * @param player
     * @param world
     * @return Last known Location, or null if not found/stored
     */
    public static Location readLastLocationOfWorldGroup(Player player, World world) {
        String sharedWorldName = WorldConfig.get(world).inventory.getSharedWorldName();
        WorldConfig sharedWorldConfig = WorldConfig.get(sharedWorldName);
        PlayerFile sharedPlayerFile = new PlayerFile(player, sharedWorldConfig);
        if (!sharedPlayerFile.exists()) {
            return null;
        }

        CommonTagCompound data = sharedPlayerFile.read(player);
        UUID lastWorldUUID = data.getValue(DATA_TAG_LASTWORLD, UUID.class);
        if (lastWorldUUID == null) {
            return null;
        }
        World lastWorld = Bukkit.getWorld(lastWorldUUID);
        if (lastWorld == null) {
            return null;
        }

        // For the world we found, read the location of the player
        // If this is the same world we have already loaded before, sharedPlayerFile can be re-used
        if (lastWorld == sharedWorldConfig.getWorld()) {
            return readLastLocation(lastWorld, data);
        } else {
            return readLastLocation(player, lastWorld);
        }
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
        if (posFile.exists()) {
            CommonTagCompound data = posFile.read(player);
            return readLastLocation(world, data);
        } else {
            return null;
        }
    }

    private static Location readLastLocation(World world, CommonTagCompound data) {
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

    /**
     * Removes all set mob effects for a human, sending entity effect remove packets to a player
     * for player human entities.
     * 
     * @param human
     */
    private static void resetCurrentMobEffects(HumanEntity human) {
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
    }

    /**
     * Resets the state associated with 'clear inventory' rules. This includes:
     * <ul>
     * <li>Attributes applied</li>
     * <li>Potion effects / mob effects</li>
     * <li>Inventory</li>
     * <li>Experience, Health, Hunger, Exhaustion</li>
     * </ul>
     * 
     * @param human
     * @param firstTimeLoad
     */
    private static void resetState(HumanEntity human) {
        // Clear mob effects
        resetCurrentMobEffects(human);

        // Clear attributes
        EntityLivingHandle livingHandle = EntityLivingHandle.fromBukkit(human);
        NBTUtil.resetAttributes(human);
        livingHandle.resetAttributes();

        // Clear inventory
        human.getInventory().clear();

        // Reset health to maximum
        {
            CommonLivingEntity<?> cLivingEntity = new CommonLivingEntity<HumanEntity>(human);
            cLivingEntity.setHealth(cLivingEntity.getMaxHealth());
        }

        // Reset experience and other stuff
        if (human instanceof Player) {
            final Player player = (Player) human;
            player.setExp(0.0f);
            player.setTotalExperience(0);
            player.setLevel(0);
            player.setFoodLevel(20);
            player.setSaturation(5.0f);
            player.setExhaustion(0.0f);
            player.setFireTicks(0);
            PlayerRespawnPoint.NONE.applyToPlayer(player);

            // Resend items (PATCH)
            CommonUtil.nextTick(new Runnable() {
                @Override
                public void run() {
                    player.updateInventory();
                }
            });
        }
    }

    /**
     * Handles post loading of an Entity
     * 
     * @param human that got loaded
     */
    private static void postLoad(HumanEntity human) {
        if (WorldConfig.get(human.getWorld()).clearInventory) {
            resetState(human);
        }
    }

    /**
     * Applies the player states in the world to the player specified
     * 
     * @param player to set the states for
     */
    public static void refreshState(Player player) {
        // If player has the 'keep inventory' perm, don't change anything about the inventory
        if (MyWorlds.keepInventoryPermissionEnabled && Permission.GENERAL_KEEPINV.has(player)) {
            return;
        }

        // If inventory logic not enabled, only do the post-load logic (clear inventory rule)
        if (!MyWorlds.useWorldInventories) {
            postLoad(player);
            return;
        }

        try {
            final PlayerFileCollection files = new PlayerFileCollection(player, player.getWorld());
            final CommonTagCompound playerData = files.currentFile.read(player);

            files.log("refreshing state");

            CommonPlayer commonPlayer = CommonEntity.get(player);
            EntityPlayerHandle playerHandle = EntityPlayerHandle.fromBukkit(player);

            // First, clear previous player information when loading involves adding new elements
            resetState(player);

            // Refresh attributes
            // Note: must be done before loading inventory, since this sets
            //       the base attributes for armor.
            if (playerData.containsKey("Attributes")) {
                NBTUtil.loadAttributes(player, playerData.get("Attributes", CommonTagList.class));
            }

            // Load the data
            NBTUtil.loadInventory(player.getInventory(), playerData.createList("Inventory"));
            playerHandle.setExp(playerData.getValue("XpP", 0.0f));
            playerHandle.setExpLevel(playerData.getValue("XpLevel", 0));
            playerHandle.setExpTotal(playerData.getValue("XpTotal", 0));

            {
                final double maxHealth = commonPlayer.getMaxHealth();
                final double health;
                if (playerData.containsKey("HealF")) {
                    // Legacy stuff
                    Float f = playerData.getValue("HealF", float.class);
                    health = (f == null) ? maxHealth : f.doubleValue();
                } else {
                    // Supports short, float and possibly double -> double
                    health = playerData.getValue("Health", maxHealth);
                }
                commonPlayer.setHealth(Math.min(maxHealth, health));
            }

            // Initialize spawn point
            PlayerRespawnPoint respawnPoint = PlayerRespawnPoint.fromNBT(playerData);
            if (MWPlayerDataController.isValidRespawnPoint(player.getWorld(), respawnPoint)) {
                respawnPoint.applyToPlayer(player);
            }
            playerHandle.setSpawnForced(playerData.getValue("SpawnForced", false));

            NBTUtil.loadFoodMetaData(playerHandle.getFoodDataRaw(), playerData);
            NBTUtil.loadInventory(player.getEnderChest(), playerData.createList("EnderItems"));

            if (playerData.containsKey("playerGameType")) {
                player.setGameMode(GameMode.getByValue(playerData.getValue("playerGameType", 1)));
            }

            // data.getValue("Bukkit.MaxHealth", (float) commonPlayer.getMaxHealth());

            // Load Mob Effects
            {
                Map<MobEffectListHandle, MobEffectHandle> effects = playerHandle.getMobEffects();
                if (playerData.containsKey("ActiveEffects")) {
                    CommonTagList taglist = playerData.createList("ActiveEffects");
                    for (int i = 0; i < taglist.size(); ++i) {
                        MobEffectHandle mobEffect = NBTUtil.loadMobEffect((CommonTagCompound) taglist.get(i));
                        effects.put(mobEffect.getEffectList(), mobEffect);
                    }
                }
                playerHandle.setUpdateEffects(true);
            }

            // Perform post loading
            postLoad(player);

            // Send add messages for all (new) effects
            for (MobEffectHandle effect : EntityPlayerHandle.fromBukkit(player).getMobEffects().values()) {
                PacketUtil.sendPacket(player, PacketType.OUT_ENTITY_EFFECT_ADD.newInstance(player.getEntityId(), effect));
            }

            // Resend equipment of the players that see this player.
            // Otherwise equipment stays visible that was there before.
            Chunk chunk = player.getLocation().getChunk();
            for (Player viewer : player.getWorld().getPlayers()) {
                if (viewer != player && PlayerUtil.isChunkVisible(viewer, chunk)) {
                    for (EquipmentSlot slot : EquipmentSlot.values()) {
                        PacketPlayOutEntityEquipmentHandle packet = PacketPlayOutEntityEquipmentHandle.createNew(
                                player.getEntityId(), slot, Util.getEquipment(player, slot));
                        PacketUtil.sendPacket(viewer, packet);
                    }
                }
            }
        } catch (Throwable t) {
            MyWorlds.plugin.getLogger().log(Level.WARNING, "Failed to load player data for " + player.getName(), t);
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
            final PlayerFileCollection files = new PlayerFileCollection(player, player.getWorld());
            CommonTagCompound savedData = NBTUtil.saveEntity(player, null);

            files.log("saving data after respawn");

            // We store this entire Bukkit tag + experience information!
            CommonTagCompound bukkitTag = savedData.get("bukkit", CommonTagCompound.class);
            boolean keepLevel = (bukkitTag != null && bukkitTag.getValue("keepLevel", false));
            if (!keepLevel) {
                if (bukkitTag != null) {
                    bukkitTag.putValue("newTotalExp", 0);
                    bukkitTag.putValue("newLevel", 0);
                }
                savedData.putValue("XpP", 0.0f);
                savedData.putValue("XpTotal", 0);
                savedData.putValue("XpLevel", 0);
            }

            // Disable bed spawn if not enabled for that world
            // Also removes a stale bed spawn point which can't be used there
            removeInvalidBedSpawn(player.getWorld(), savedData);

            // If gamerule keep inventory is active for the world the player died in, also save the
            // original items in the inventory
            // EDIT: Not needed. Player inventory is already wiped (according to server logic)
            //       before this respawn saving logic gets executed
            /*
            if (!"true".equals(player.getWorld().getGameRuleValue("keepInventory"))) {
                clearInventoryNBTData(savedData);
            }
            */

            // Remove potion effects
            savedData.remove("ActiveEffects");
            savedData.remove("AbsorptionAmount");

            // Replace health/damage info
            CommonPlayer playerEntity = CommonEntity.get(player);
            if (SAVE_HEAL_F) {
                savedData.putValue("HealF", (float) playerEntity.getMaxHealth());
            } else if (savedData.containsKey("HealF")) {
                savedData.remove("HealF");
            }
            savedData.putValue("Health", (float) playerEntity.getMaxHealth());
            savedData.putValue("HurtTime", (short) 0);
            savedData.putValue("DeathTime", (short) 0);
            savedData.putValue("AttackTime", (short) 0);
            savedData.putListValues("Motion", 0.0, 0.0, 0.0);

            // Unimportant
            savedData.remove("FallFlying");
            savedData.remove("FallDistance");
            savedData.remove("Fire");
            savedData.remove("Air");
            savedData.remove("OnGround");
            savedData.remove("PortalCooldown");
            savedData.remove("SleepingX");
            savedData.remove("SleepingY");
            savedData.remove("SleepingZ");
            savedData.remove("ShoulderEntityLeft");
            savedData.remove("ShoulderEntityRight");
            savedData.remove("SleepTimer");

            // Now, go ahead and save this data
            files.currentFile.write(savedData);

            // Finally, we need to update where the player is at right now
            // To do so, we will write a new main world where the player is meant to be
            // This operation is a bit optional at this point, but it avoids possible issues in case of crashes
            // This is only needed if a main player data file doesn't exist
            // (this should in theory never happen either...player is not joining)
            if (files.mainWorldFile.exists()) {
                files.mainWorldFile.update(player, new DataUpdater() {
                    @Override
                    public void update(CommonTagCompound data) {
                        data.putUUID("World", respawnLocation.getWorld().getUID());
                    }
                });
            }
        } catch (Throwable t) {
            MyWorlds.plugin.getLogger().log(Level.WARNING, "Failed to save player respawned data for " + player.getName(), t);
        }
    }

    @Override
    public CommonTagCompound onLoad(final Player player) {
        try {
            final PlayerFileCollection files = new PlayerFileCollection(player, player.getWorld());

            CommonTagCompound mainWorldData = null;
            boolean hasPlayedBefore = false;

            // If a main world player data file exists, then the player has been on the server before
            hasPlayedBefore = files.mainWorldFile.exists();

            // Find out where to find the save file
            // No need to check for this if not using world inventories - it is always the main file then
            if (MyWorlds.useWorldInventories && hasPlayedBefore && !MyWorlds.forceJoinOnMainWorld) {
                try {
                    // Allow switching worlds and positions
                    mainWorldData = files.mainWorldFile.read(player);
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
                playerData = files.currentFile.read(player);
            }

            // Disable bed spawn if not enabled for that world
            removeInvalidBedSpawn(player.getWorld(), playerData);

            // When main world spawning is forced, reset location to there
            if (!hasPlayedBefore || MyWorlds.forceJoinOnMainWorld) {
                setLocation(playerData, WorldManager.getSpawnLocation(MyWorlds.getMainWorld()));
            }

            // If for this world the inventory is cleared, clear relevant data in the NBT that should be removed
            World playerCurrentWorld = Bukkit.getWorld(((mainWorldData != null) ? mainWorldData : playerData).getUUID("World"));
            if (playerCurrentWorld != null && WorldConfig.get(playerCurrentWorld).clearInventory) {
                clearInventoryNBTData(playerData);
                playerData.remove("ActiveEffects");
                playerData.putValue("XpLevel", 0);
                playerData.putValue("XpTotal", 0);
                playerData.putValue("XpP", 0.0F);
                playerData.remove("HealF");
                playerData.remove("Health");

                // Save this player data back to file to make sure clear inventory is adhered
                files.currentFile.write(playerData);
            }

            // Minecraft bugfix here: Clear mob/potion effects BEFORE loading the data
            // This resolves issues with effects staying behind
            resetCurrentMobEffects(player);

            // Load the entity using the player data compound
            NBTUtil.loadEntity(player, playerData);

            // Bukkit bug: entityplayer.e(tag) -> b(tag) -> craft.readExtraData(tag) which instantly sets it
            // Make sure the player is marked as being new
            PlayerUtil.setHasPlayedBefore(player, hasPlayedBefore);
            if (hasPlayedBefore) {
                // As specified in the WorldNBTStorage implementation, set this
                PlayerUtil.setFirstPlayed(player, files.mainWorldFile.lastModified());
            }

            // DISABLED: NPE on some servers when inventory clear is called this early
            // postLoad(player);

            // When set as flying, there appears to be a problem or bug where this is reset
            // This causes the player to come falling down upon (re-)join, which is really annoying
            // For now this is fixed by explicitly calling setFlying one tick later
            if (playerData.containsKey("abilities")) {
                CommonTagCompound abilities = (CommonTagCompound) playerData.get("abilities");
                if (abilities.getValue("flying", false)) {
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
        } catch (Throwable t) {
            MyWorlds.plugin.getLogger().log(Level.WARNING, "Failed to load player data for " + player.getName(), t);
            return super.onLoad(player);
        }
    }

    @Override
    public void onSave(final Player player) {
        try {
            final PlayerFileCollection files;
            synchronized (worldToSaveTo) {
                files = new PlayerFileCollection(player, worldToSaveTo.getOrDefault(player, player.getWorld()));
            }

            final CommonTagCompound savedData = NBTUtil.saveEntity(player, null);

            // Disable bed spawn if not enabled for that world
            removeInvalidBedSpawn(player.getWorld(), savedData);

            files.log("saving data");

            final Location loc = player.getLocation();
            if (files.isSingleDataFile()) {
                // Append the Last Pos/Rot to the data
                savedData.putListValues(DATA_TAG_LASTPOS, loc.getX(), loc.getY(), loc.getZ());
                savedData.putListValues(DATA_TAG_LASTROT, loc.getYaw(), loc.getPitch());
            } else {
                // Append original last position (if available) to the data
                if (files.currentFile.exists()) {
                    CommonTagCompound data = files.currentFile.read(player);
                    if (data.containsKey(DATA_TAG_LASTPOS)) {
                        savedData.put(DATA_TAG_LASTPOS, data.get(DATA_TAG_LASTPOS));
                    }
                    if (data.containsKey(DATA_TAG_LASTROT)) {
                        savedData.put(DATA_TAG_LASTROT, data.get(DATA_TAG_LASTROT));
                    }
                }

                // Write the Last Pos/Rot to the official world file instead
                files.positionFile.update(player, new DataUpdater() {
                    @Override
                    public void update(CommonTagCompound data) {
                        data.putListValues(DATA_TAG_LASTPOS, loc.getX(), loc.getY(), loc.getZ());
                        data.putListValues(DATA_TAG_LASTROT, loc.getYaw(), loc.getPitch());
                    }
                });
            }

            // Store last world player was on in the same file also storing inventory state
            savedData.putValue(DATA_TAG_LASTWORLD, loc.getWorld().getUID());

            // Save data to the destination file
            files.currentFile.write(savedData);

            // Write the current world name of the player to the save file of the main world
            if (!files.isMainWorld()) {
                // Update the world in the main file
                files.mainWorldFile.update(player, new DataUpdater() {
                    @Override
                    public void update(CommonTagCompound data) {
                        data.put("Pos", savedData.get("Pos"));
                        data.put("Rotation", savedData.get("Rotation"));
                        data.putUUID("World", player.getWorld().getUID());
                    }
                });
            }
        } catch (Throwable t) {
            MyWorlds.plugin.getLogger().log(Level.WARNING, "Failed to save player data for " + player.getName(), t);
        }
    }

    private static void clearInventoryNBTData(CommonTagCompound playerData) {
        // Create an empty CommonTagList with the right type information, by adding a compound and removing it
        // There is no good api in BKCommonLib to make an empty list of a given element type (TODO!)
        {
            CommonTagList emptyInventory = new CommonTagList();
            emptyInventory.add(new CommonTagCompound());
            emptyInventory.remove(0);
            playerData.put("Inventory", emptyInventory);
        }

        playerData.remove("Attributes");
        playerData.remove("SelectedItemSlot");
    }

    private static void removeInvalidBedSpawn(World world, CommonTagCompound playerData) {
        PlayerRespawnPoint current = PlayerRespawnPoint.fromNBT(playerData);
        if (!current.isNone() && !WorldConfig.get(current.getWorld()).bedRespawnEnabled) {
            PlayerRespawnPoint.NONE.toNBT(playerData);
        } else if (!isValidRespawnPoint(world, current)) {
            PlayerRespawnPoint.NONE.toNBT(playerData);
        }
    }

    public interface DataUpdater {
        public void update(CommonTagCompound data);
    }

    public static class PlayerFile {
        public final OfflinePlayer player;
        public final File file;

        public PlayerFile(OfflinePlayer player, String worldName) {
            this(player, WorldConfig.get(worldName));
        }

        public PlayerFile(OfflinePlayer player, WorldConfig worldConfig) {
            this.player = player;
            this.file = worldConfig.getPlayerData(player);
            this.file.getParentFile().mkdirs();
        }

        public boolean exists() {
            return file.exists();
        }

        public long lastModified() {
            return file.lastModified();
        }

        public CommonTagCompound readIfExists(OfflinePlayer player) {
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
            CommonTagCompound data = this.readIfExists(player);
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
    }

    public static class PlayerFileCollection {
        public final OfflinePlayer player;
        public PlayerFile mainWorldFile; /* Stores the name of the main world the player is on */
        public PlayerFile positionFile;  /* Stores the position of the player on a particular world */
        public PlayerFile currentFile;   /* Stores the inventory and data of the player on a particular world. Merge/split rules apply. */

        public PlayerFileCollection(OfflinePlayer player, World world) {
            this.player = player;
            this.mainWorldFile = new PlayerFile(player, MyWorlds.storeInventoryInMainWorld ?
                    WorldConfig.getMain() : WorldConfig.getVanillaMain());

            setCurrentWorld(world);
        }

        /**
         * Sets the current world the player is in, switching the file locations
         * 
         * @param currentWorld the player is in
         */
        public void setCurrentWorld(World currentWorld) {
            WorldConfig currentWorldConfig = WorldConfig.get(currentWorld);
            this.positionFile = new PlayerFile(player, currentWorldConfig);
            if (MyWorlds.useWorldInventories) {
                this.currentFile = new PlayerFile(player, currentWorldConfig.inventory.getSharedWorldName());
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
}
