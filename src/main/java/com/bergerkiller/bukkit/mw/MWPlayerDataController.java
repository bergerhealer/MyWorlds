package com.bergerkiller.bukkit.mw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.wrappers.Holder;
import com.bergerkiller.bukkit.mw.playerdata.InventoryEditRecovery;
import com.bergerkiller.bukkit.mw.playerdata.PlayerDataBootstrap;
import com.bergerkiller.bukkit.mw.utils.KeyedLockMap;
import com.bergerkiller.generated.net.minecraft.world.entity.ai.attributes.AttributeMapBaseHandle;
import com.bergerkiller.generated.net.minecraft.world.entity.ai.attributes.AttributeModifiableHandle;
import com.bergerkiller.mountiplex.reflection.resolver.Resolver;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

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
import com.bergerkiller.bukkit.common.utils.StreamUtil;
import com.bergerkiller.bukkit.common.wrappers.PlayerRespawnPoint;
import com.bergerkiller.bukkit.mw.playerdata.LastPlayerPositionList;
import com.bergerkiller.bukkit.mw.playerdata.PlayerDataFile;
import com.bergerkiller.bukkit.mw.playerdata.PlayerDataFileCollection;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutEntityEquipmentHandle;
import com.bergerkiller.generated.net.minecraft.server.level.EntityPlayerHandle;
import com.bergerkiller.generated.net.minecraft.world.effect.MobEffectHandle;
import com.bergerkiller.generated.net.minecraft.world.effect.MobEffectListHandle;
import com.bergerkiller.generated.net.minecraft.world.entity.EntityLivingHandle;

public class MWPlayerDataController extends PlayerDataController {
    /** Root compound data tag path where MyWorlds stores all metadata in player data files */
    public static final String DATA_TAG_ROOT = "MyWorlds";
    /**
     * At this position in the main world's player data file MyWorlds stores the last positions
     * and world UUID's a player was at. This replaces legacy 'positionFile' logic.<br>
     * <br>
     * Stored within {@link #DATA_TAG_ROOT} compound.
     */
    public static final String DATA_TAG_LAST_POSITIONS = "lastPlayerPositions";
    /**
     * At this position in the main world's player data file MyWorlds stores whether the data
     * was saved back when MyWorlds multiple-world-inventories feature was turned off. In that
     * case the data in the main file should be migrated to the world it is actually for,
     * if currently this feature is enabled.<br>
     * <br>
     * If this tag and the LEGACY_DATA_TAG_LASTWORLD tag are absent or refer to the main
     * world itself it is assumed the data must be migrated first.<br>
     * <br>
     * Stored within {@link #DATA_TAG_ROOT} compound.
     */
    public static final String DATA_TAG_IS_SELF_CONTAINED = "isSelfContained";
    /**
     * Stores synchronization locks per player, so that asynchronous saves/loads don't cause corruption
     */
    public static final KeyedLockMap<String> PLAYER_PROFILE_LOCKS = new KeyedLockMap<>();

    // Legacy stuff! Used purely to import older player data and migrate to the 'lastPlayerPositions' api.
    // The playerWorld tag stores the world the position/rotation is for, matching the data file
    // This is stored in the shared world group 'storage' world
    public static final String LEGACY_DATA_TAG_LASTPOS = "MyWorlds.playerPos";
    public static final String LEGACY_DATA_TAG_LASTROT = "MyWorlds.playerRot";
    public static final String LEGACY_DATA_TAG_LASTWORLD = "MyWorlds.playerWorld";

    /**
     * The data tag below which player inventory data is saved
     */
    public static final String VANILLA_INVENTORY_TAG = "Inventory";
    /**
     * The data tag below which player ender chest inventory data is saved
     */
    public static final String VANILLA_ENDER_CHEST_TAG = "EnderItems";

    private static final boolean SAVE_HEAL_F = Common.evaluateMCVersion("<=", "1.8.8");
    private static final boolean CAN_RESET_ATTRIBUTES = Common.hasCapability("Common:Attributes:RemoveAllModifiers");

    private static final Map<Player, World> worldToSaveTo = new IdentityHashMap<>();
    private static final Map<Player, LastPlayerPositionList> playerLastLocations = new IdentityHashMap<>();
    private final MyWorlds plugin;

    static {
        // Ensure <clinit> is done to avoid problems at server shutdown
        PlayerDataBootstrap.init();
        PlayerRespawnPoint.NONE.toString();
    }

    public MWPlayerDataController(MyWorlds plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        PLAYER_PROFILE_LOCKS.startCleanup(plugin);
        this.assign();
    }

    private static Object getLock(OfflinePlayer player) {
        return PLAYER_PROFILE_LOCKS.getLock(player.getUniqueId().toString());
    }

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
        // This saves way too much. Only save data we manage
        //CommonUtil.savePlayer(player);
        MyWorlds.plugin.getPlayerDataController().onSave(player);
        synchronized (worldToSaveTo) {
            worldToSaveTo.remove(player);
        }
    }

    /**
     * Creates new player data information as if the player just joined the server.
     * 
     * @param player to generate information about
     * @return empty data
     */
    public static CommonTagCompound createEmptyData(Player player) {
        return PlayerDataFile.createEmptyData(player);
    }

    /**
     * Attempts to read the bed spawn position on a specific World
     * 
     * @param player to get the bed spawn location for
     * @param world to get the bed spawn location for
     * @return Bed spawn location, or NONE if not set / stored
     */
    public static PlayerRespawnPoint readRespawnPoint(Player player, World world) {
        // If world inventories are disabled, then there is only ever one global bed spawn
        // And this bed spawn will already have been set for the player. So use that.
        if (!MyWorlds.useWorldInventories) {
            return PlayerRespawnPoint.forPlayer(player);
        }

        // What world is the respawn point information actually stored on?
        WorldConfig worldConfigStoringInventory = WorldConfig.getIfExists(WorldConfig.get(world).inventory.getSharedWorldName());
        if (worldConfigStoringInventory == null) {
            return PlayerRespawnPoint.NONE;
        }

        // If player is already using this inventory, then there is no use reading it from disk
        if (worldConfigStoringInventory.inventory.contains(player.getWorld())) {
            return PlayerRespawnPoint.forPlayer(player);
        }

        synchronized (getLock(player)) {
            PlayerDataFile posFile = new PlayerDataFile(player, worldConfigStoringInventory);
            if (!posFile.exists()) {
                return PlayerRespawnPoint.NONE;
            }

            CommonTagCompound data = posFile.read(player);
            PlayerRespawnPoint respawn = PlayerRespawnPoint.fromNBT(data);
            return isValidRespawnPoint(world, respawn) ? respawn : PlayerRespawnPoint.NONE;
        }
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
        synchronized (getLock(player)) {
            // Try to read this information
            PlayerDataFile mainWorldFile = PlayerDataFile.mainFile(player);
            CommonTagCompound mainWorldData = mainWorldFile.readIfExists();
            if (mainWorldData != null) {
                World world = Bukkit.getWorld(mainWorldData.getUUID("World"));
                if (world != null) {
                    return world;
                }
            }

            // Return the main world configured
            return MyWorlds.getMainWorld();
        }
    }

    /**
     * Gets the last-known position of a Player in a world, or any of the worlds set
     * as the world rejoin group of that world. Based on time. If the player had
     * last died on the last world it was on, then returns null to force a respawn.
     *
     * @param player
     * @param possibleWorldConfigs Worlds to check for a last position
     * @return Last known Location, or null if not found/stored
     */
    public static Location readLastLocationOfWorldGroup(Player player, List<WorldConfig> possibleWorldConfigs) {
        // Find all positions known for the player
        // Import legacy positions of all worlds we need to check
        LastPlayerPositionList lastPositions = readLastPlayerPositions(player, possibleWorldConfigs);
        for (LastPlayerPositionList.LastPosition pos : lastPositions.all(true)) {
            if (pos.hasDied()) {
                break; // Fail instantly if the player last died here
            }

            World posWorld = pos.getWorld();
            if (posWorld == null) {
                continue; // Not loaded
            }

            for (WorldConfig wc : possibleWorldConfigs) {
                if (wc.getWorld() == posWorld) {
                    Location loc = pos.getLocation();
                    if (loc == null) {
                        break; // Not loaded? Eh?
                    }
                    if (!verifyLastLocationValid(loc, player)) {
                        lastPositions = lastPositions.clone();
                        lastPositions.removeForWorld(wc);
                        storeLastPlayerPositions(player, lastPositions);
                        break;
                    }
                    return loc;
                }
            }
        }

        // Unknown
        return null;
    }

    /**
     * Attempts to read the last known Player position on a specific World
     *
     * @param player to get the last position for
     * @param world to get the last position for
     * @return Last known Location, or null if not found/stored
     */
    public static Location readLastLocation(Player player, World world) {
        return readLastLocation(player, world, false);
    }

    /**
     * Attempts to read the last known Player position on a specific World
     * 
     * @param player to get the last position for
     * @param world to get the last position for
     * @param ignoreDied True to ignore that the player last died in that world
     * @return Last known Location, or null if not found/stored
     */
    public static Location readLastLocation(Player player, World world, boolean ignoreDied) {
        WorldConfig config = WorldConfig.get(world);
        LastPlayerPositionList posList = readLastPlayerPositions(player, Collections.singletonList(config));
        LastPlayerPositionList.LastPosition pos = posList.getForWorld(config);
        if (pos == null || pos.hasDied()) {
            return null;
        }
        Location loc = pos.getLocation();
        if (loc == null) {
            return null;
        }
        if (!verifyLastLocationValid(loc, player)) {
            posList = posList.clone();
            posList.removeForWorld(config);
            storeLastPlayerPositions(player, posList);
            return null;
        }
        return loc;
    }

    private static boolean verifyLastLocationValid(Location loc, Player player) {
        // Verify a player profile file actually exists at the destination path
        // If there is not, then the player profile was wiped out and this last position
        // information got out of sync.
        PlayerDataFile file = new PlayerDataFile(player, WorldConfig.get(loc.getWorld()));
        if (!file.exists()) {
            return false;
        }

        // TODO: Do we actually read this file to check the position as well?
        return true;
    }

    /**
     * Looks up the previous positions on worlds a Player had. Reads this information from cache,
     * or by re-reading the main world player data file.
     *
     * @param player Player for which to read or load the player positions
     *
     * @return Previous positions of this Player. Should not be modified.
     */
    public static LastPlayerPositionList readLastPlayerPositions(Player player) {
        return readLastPlayerPositions(player, WorldConfig.all());
    }

    /**
     * Looks up the previous positions on worlds a Player had. Reads this information from cache,
     * or by re-reading the main world player data file.
     *
     * @param player Player for which to read or load the player positions
     * @param worlds Worlds for which last-player position data must be made available.
     *               This is used to read last-player position data in legacy formats.
     *               If left empty, will just show data stored in the new format.
     * @return Previous positions of this Player. Should not be modified.
     */
    public static LastPlayerPositionList readLastPlayerPositions(Player player, Collection<WorldConfig> worlds) {
        synchronized (getLock(player)) {
            LastPlayerPositionList positions;
            synchronized (playerLastLocations) {
                positions = playerLastLocations.get(player);
            }
            boolean changed = false;
            if (positions == null) {
                PlayerDataFile mainWorldFile = PlayerDataFile.mainFile(player);
                CommonTagCompound data = mainWorldFile.readIfExists();
                positions = parseLastPlayerPositions(mainWorldFile, data);
                changed = true;
            }

            // Parse legacy data and include it into the results
            for (WorldConfig config : worlds) {
                if (!positions.containsWorld(config)) {
                    // Try to see if data exists in a player data file on that particular world
                    PlayerDataFile file = new PlayerDataFile(player, config);
                    CommonTagCompound posData = parseLegacyLastPosition(file, file.readIfExists());
                    if (posData == null && !config.isLoaded()) {
                        continue; // Skip non-loaded worlds
                    }

                    // Avoid mutating the original
                    if (!changed) {
                        positions = positions.clone();
                        changed = true;
                    }

                    if (posData != null) {
                        positions.add(posData);
                    } else {
                        positions.addNoPositionSlot(config);
                    }
                }
            }

            if (changed) {
                playerLastLocations.put(player, positions);
            }

            return positions;
        }
    }

    private static void storeLastPlayerPositions(Player player, LastPlayerPositionList positions) {
        synchronized (playerLastLocations) {
            playerLastLocations.put(player, positions);
        }
    }

    private static LastPlayerPositionList parseLastPlayerPositions(PlayerDataFile mainWorldPlayerFile, CommonTagCompound mainWorldPlayerData) {
        if (mainWorldPlayerData != null) {
            // New system of storing a list of positions in the main world data file
            CommonTagCompound myworldsData = mainWorldPlayerData.get(DATA_TAG_ROOT, CommonTagCompound.class);
            if (myworldsData != null) {
                CommonTagList result = myworldsData.get(DATA_TAG_LAST_POSITIONS, CommonTagList.class);
                if (result != null) {
                    LastPlayerPositionList positions = new LastPlayerPositionList(result);
                    positions.cleanupMissingWorldNoPositionSlots();
                    return positions;
                }
            }

            // File itself might store the last-known position of the player on the main world
            // If so, keep it!
            CommonTagCompound lastPosOnMainWorld = parseLegacyLastPosition(mainWorldPlayerFile, mainWorldPlayerData);
            if (lastPosOnMainWorld != null) {
                CommonTagList result = new CommonTagList();
                result.add(lastPosOnMainWorld);
                return new LastPlayerPositionList(result);
            }
        }
        return new LastPlayerPositionList();
    }

    private static CommonTagCompound parseLegacyLastPosition(PlayerDataFile playerFile, CommonTagCompound worldPlayerData) {
        if (worldPlayerData != null) {
            CommonTagList posValues = worldPlayerData.get(LEGACY_DATA_TAG_LASTPOS, CommonTagList.class);
            if (posValues != null && posValues.size() == 3) {
                // Generate a new compound in the same format as stored in the 'lastPlayerPositions' compound list
                CommonTagCompound posData = new CommonTagCompound();

                // World UUID. If world is actually loaded.
                {
                    World world = playerFile.world.getWorld();
                    if (world != null) {
                        posData.putUUID(LastPlayerPositionList.DATA_TAG_WORLD, world.getUID());
                    }
                }

                // World name (fallback, or for if world isn't loaded)
                posData.putValue(LastPlayerPositionList.DATA_TAG_WORLD_NAME, playerFile.world.worldname);

                // Timestamp the player last played on this World. For this we can re-use Bukkit's
                // 'lastPlayed' timestamp. If absent, we'll have to use the data file's last modification
                // time
                long lastPlayedMillis;
                {
                    CommonTagCompound bukkit = worldPlayerData.get("bukkit", CommonTagCompound.class);
                    if (bukkit != null && bukkit.containsKey("lastPlayed")) {
                        // Bukkit stored it
                        lastPlayedMillis = bukkit.getValue("lastPlayed", 0L);
                    } else if (playerFile.file.exists()) {
                        // Try file modification timestamp. 0 if some weirdness happens.
                        lastPlayedMillis = playerFile.file.lastModified();
                    } else {
                        // Don't know and can't know. Use 0 to force it to be used as a last-resort
                        lastPlayedMillis = 0;
                    }
                }
                posData.putValue(LastPlayerPositionList.DATA_TAG_TIME, lastPlayedMillis);

                // Position coordinates
                posData.put(LastPlayerPositionList.DATA_TAG_POS, posValues);

                // If rotation is there, also put it
                CommonTagList rotInfo = worldPlayerData.get(LEGACY_DATA_TAG_LASTROT, CommonTagList.class);
                if (rotInfo != null) {
                    posData.put(LastPlayerPositionList.DATA_TAG_ROT, rotInfo);
                }

                return posData;
            }
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

        // Reset current mob effects applied to the player
        // Notify the player of these effects being gone so they don't ghost
        {
            Map<Holder<MobEffectListHandle>, MobEffectHandle> effects = livingHandle.getMobEffects();
            if (human instanceof Player) {
                // Send mob effect removal messages
                Player player = (Player) human;
                for (Holder<MobEffectListHandle> effect : effects.keySet()) {
                    PacketUtil.sendPacket(player, PacketType.OUT_ENTITY_EFFECT_REMOVE.newInstance(player.getEntityId(), effect));
                }
            }
            effects.clear();
        }

        // Reset all attributes of the player to the defaults
        // The reset method calls setDirty(), which will cause a synchronization later
        if (CAN_RESET_ATTRIBUTES && human instanceof Player) {
            resetAttributes(livingHandle, (Player) human);
        }
    }

    //TODO: Make inline when bkcl 1.21 or later is a hard-dep
    private static void resetAttributes(EntityLivingHandle handle, Player player) {
        AttributeMapBaseHandle map = handle.getAttributeMap();
        Collection<AttributeModifiableHandle> allAttributes = new ArrayList<>(map.getAllAttributes());
        allAttributes.forEach(AttributeModifiableHandle::removeAllModifiers);
        PacketUtil.sendPacket(player, PacketType.OUT_ENTITY_UPDATE_ATTRIBUTES.newInstance(player.getEntityId(), map.getSynchronizedAttributes()));
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
    private void postLoad(HumanEntity human) {
        if (WorldConfig.get(human.getWorld()).clearInventory) {
            resetState(human);
        }
    }

    /**
     * Applies the player states in the world to the player specified
     * 
     * @param player to set the states for
     */
    public void refreshState(Player player) {
        // If player has the 'keep inventory' perm, don't change anything about the inventory
        if (MyWorlds.keepInventoryPermissionEnabled && Permission.GENERAL_KEEPINV.has(player)) {
            return;
        }

        synchronized (getLock(player)) {
            // If inventory logic not enabled, only do the post-load logic (clear inventory rule)
            if (!MyWorlds.useWorldInventories) {
                postLoad(player);
                return;
            }

            try {
                final PlayerDataFileCollection files = new PlayerDataFileCollection(player, player.getWorld());
                CommonTagCompound playerData = files.currentFile.read(player);

                // Migrate loaded data to the newest version so that it works on the current version of the server
                playerData = NBTUtil.migratePlayerProfileData(playerData);

                files.log("refreshing state");

                CommonPlayer commonPlayer = CommonEntity.get(player);
                EntityPlayerHandle playerHandle = EntityPlayerHandle.fromBukkit(player);

                // First, clear previous player information when loading involves adding new elements
                resetState(player);

                // Refresh attributes
                // Note: must be done before loading inventory, since this sets
                //       the base attributes for armor.
                if (playerData.containsKey("attributes")) {
                    NBTUtil.loadAttributes(player, playerData.get("attributes", CommonTagList.class));
                } else if (playerData.containsKey("Attributes")) {
                    NBTUtil.loadAttributes(player, playerData.get("Attributes", CommonTagList.class));
                }

                // Load the data
                NBTUtil.loadInventory(player.getInventory(), playerData.createList(VANILLA_INVENTORY_TAG));
                player.getInventory().setHeldItemSlot(playerData.getValue("SelectedItemSlot", 0));
                playerHandle.setExp(playerData.getValue("XpP", 0.0f));
                playerHandle.setExpLevel(playerData.getValue("XpLevel", 0));
                playerHandle.setExpTotal(playerData.getValue("XpTotal", 0));
                playerHandle.setOnGround(playerData.getValue("OnGround", false));
                player.setRemainingAir(playerData.getValue("Air", (short) player.getMaximumAir()));
                player.setFireTicks(playerData.getValue("Fire", (short) 0));
                player.setFallDistance(playerData.getValue("FallDistance", 0.0f));

                float absorptionAmount = playerData.getValue("AbsorptionAmount", 0.0f);
                try {
                    playerHandle.setAbsorptionAmount(absorptionAmount);
                } catch (Throwable t) {
                    /* Until BKCL 1.19.3-v2 is a hard-dep, we need this. */
                    try {
                        java.lang.reflect.Method m;
                        if (Common.evaluateMCVersion(">=", "1.18")) {
                            m = Resolver.resolveAndGetDeclaredMethod(EntityLivingHandle.T.getType(),
                                    "setAbsorptionAmount", float.class);
                        } else {
                            m = Resolver.resolveAndGetDeclaredMethod(EntityLivingHandle.T.getType(),
                                    "setAbsorptionHearts", float.class);
                        }
                        m.setAccessible(true);
                        m.invoke(playerHandle.getRaw(), absorptionAmount);
                    } catch (Throwable t2) {
                        plugin.getLogger().log(Level.WARNING, "Failed to apply absorption. Update BKCL?", t2);
                    }
                }

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
                NBTUtil.loadInventory(player.getEnderChest(), playerData.createList(VANILLA_ENDER_CHEST_TAG));

                if (playerData.containsKey("playerGameType")) {
                    player.setGameMode(GameMode.getByValue(playerData.getValue("playerGameType", 1)));
                }

                // data.getValue("Bukkit.MaxHealth", (float) commonPlayer.getMaxHealth());

                // Load Mob Effects
                {
                    Map<Holder<MobEffectListHandle>, MobEffectHandle> effects = playerHandle.getMobEffects();
                    CommonTagList effectsTagList = readPotionEffects(playerData);
                    if (effectsTagList != null) {
                        for (int i = 0; i < effectsTagList.size(); ++i) {
                            MobEffectHandle mobEffect = NBTUtil.loadMobEffect((CommonTagCompound) effectsTagList.get(i));
                            if (mobEffect != null) {
                                effects.put(mobEffect.getEffectList(), mobEffect);
                            }
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

                // What equipment slots does this player support?
                List<EquipmentSlot> playerSupportedSlots;
                if (Common.hasCapability("Common:EquipmentSlot:IsSupportedCheck")) {
                    playerSupportedSlots = Arrays.stream(EquipmentSlot.values())
                            .filter(slot -> EntityUtil.isEquipmentSupported(player, slot))
                            .collect(Collectors.toList());
                } else {
                    playerSupportedSlots = Arrays.stream(EquipmentSlot.values())
                            .filter(slot -> !slot.name().equals("BODY"))
                            .collect(Collectors.toList());
                }

                // Resend equipment of the players that see this player.
                // Otherwise equipment stays visible that was there before.
                Chunk chunk = player.getLocation().getChunk();
                for (Player viewer : player.getWorld().getPlayers()) {
                    if (viewer != player && PlayerUtil.isChunkVisible(viewer, chunk)) {
                        for (EquipmentSlot slot : playerSupportedSlots) {
                            PacketPlayOutEntityEquipmentHandle packet = PacketPlayOutEntityEquipmentHandle.createNew(
                                    player.getEntityId(), slot, EntityUtil.getEquipment(player, slot));
                            PacketUtil.sendPacket(viewer, packet);
                        }
                    }
                }
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "Failed to load player data for " + player.getName(), t);
            }
        }
    }

    /**
     * Fired when a player respawns and all it's settings will be wiped.
     * The player contains all information right before respawning.
     * All data that would be wiped should be written as being wiped.
     * This involves a manual save.
     * 
     * @param player that respawned
     * @param respawnLocation where the player respawns at
     */
    public void onRespawnSave(Player player, final Location respawnLocation) {
        synchronized (getLock(player)) {
            try {
                final PlayerDataFileCollection files = new PlayerDataFileCollection(player, player.getWorld());
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
                    resetPlayerData(savedData);
                }
                */

                // Remove potion effects
                clearPotionEffects(savedData);
                savedData.remove("AbsorptionAmount");

                // Replace health/damage info
                CommonPlayer playerEntity = CommonEntity.get(player);
                if (SAVE_HEAL_F) {
                    savedData.putValue("HealF", (float) playerEntity.getMaxHealth());
                } else if (savedData.containsKey("HealF")) {
                    savedData.remove("HealF");
                }
                savedData.putValue("Health", (float) playerEntity.getMaxHealth());
                savedData.putValue("foodLevel", 20);
                savedData.putValue("foodTickTimer", 0);
                savedData.putValue("foodExhaustionLevel", 0.0f);
                savedData.putValue("foodSaturationLevel", 5.0f);
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

                // Track the last position on this world. Mark as a grave.
                // Updates information tracked in the main world player data file
                final Consumer<CommonTagCompound> mainWorldUpdater = saveCurrentPosition(player, player.getLocation(), true);

                // Now, go ahead and save this data
                files.currentFile.write(savedData);

                // Finally, we need to update where the player is at right now
                // To do so, we will write a new main world where the player is meant to be
                // This operation is a bit optional at this point, but it avoids possible issues in case of crashes
                // This is only needed if a main player data file doesn't exist
                // (this should in theory never happen either...player is not joining)
                if (files.mainWorldFile.exists()) {
                    files.mainWorldFile.update(player, data -> {
                        data.putUUID("World", respawnLocation.getWorld().getUID());
                        mainWorldUpdater.accept(data);
                    });
                }
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "Failed to save player respawned data for " + player.getName(), t);
            }
        }
    }

    @Override
    public CommonTagCompound onLoad(final Player player) {
        // If this is an openinv player, then we must not do the usual world-specific loading as that causes glitches
        // In that case, load the default vanilla way to avoid trouble.
        boolean loadedByInventoryEditor = false;
        if (player.getClass().getName().startsWith("com.lishid.openinv.")) {
            loadedByInventoryEditor = true;
        }

        synchronized (getLock(player)) {
            try {
                final PlayerDataFileCollection files = new PlayerDataFileCollection(player, player.getWorld());

                // If a main world player data file exists, then the player has been on the server before
                boolean hasPlayedBefore = files.mainWorldFile.exists();

                // Read the main world file first. We need this information regardless of whether or not
                // the MyWorlds inventories system is enabled. In here we store what world to send the player
                // to when multiple inventories are used. It also stores the last positions a player had on
                // various worlds.
                // If loading of this main world player profile fails, then we can't do anything more,
                // anyway.
                CommonTagCompound mainWorldData = null;
                CommonTagCompound playerData = null;
                if (hasPlayedBefore) {
                    mainWorldData = files.mainWorldFile.read(player);
                    playerData = mainWorldData; // Changed later if needed
                }

                // If player data was inventory edited, recover the original data of this world stored in a separate MyWorlds tag.
                // Write the original data back to the vanilla world, and apply the inventory-edited modified contents
                // to the world this was meant for.
                // If any of this happens, reload the main world file which may have changed
                //
                // There is no need to do this when the inventory editor itself opens the file. In that case, we're
                // already safekeeping the recovery metadata.
                if (!loadedByInventoryEditor && InventoryEditRecovery.recoverInventoryData(player, mainWorldData)) {
                    mainWorldData = files.mainWorldFile.read(player);
                    playerData = mainWorldData; // Changed later if needed
                }

                // Initialize and cache the per-player tracked 'last positions on world' information
                LastPlayerPositionList lastPlayerPositions;
                if (mainWorldData != null) {
                    // Parse from the main world data. Also imports legacy info of the main world itself.
                    lastPlayerPositions = parseLastPlayerPositions(files.mainWorldFile, mainWorldData);
                } else {
                    // No data. Initialize an empty list.
                    lastPlayerPositions = new LastPlayerPositionList();
                    lastPlayerPositions.addNoPositionSlot(files.mainWorldFile.world);
                }

                // If set to true, force player to respawn at the server spawn location as if joining
                // for the first time
                boolean respawnAtServerSpawn = !hasPlayedBefore;

                // If force-joining the main world is enabled, and we got main world data, switch
                // the stored world to the MyWorlds main world
                if (MyWorlds.forceJoinOnMainWorld && hasPlayedBefore) {
                    mainWorldData.putUUID("World", MyWorlds.getMainWorld().getUID());
                    respawnAtServerSpawn = true;
                }

                // Check world player was last on actually still exists
                World lastPlayerWorld = hasPlayedBefore ? Bukkit.getWorld(mainWorldData.getUUID("World")) : null;
                if (lastPlayerWorld == null) {
                    respawnAtServerSpawn = true;

                    // In this state we can't send a message to the player, delay it until the player
                    // has logged in
                    if (hasPlayedBefore) {
                        plugin.listener.scheduleForPlayerJoin(player, 100, Localization.WORLD_JOIN_REMOVED::message);
                    }
                }

                // Find out where to find the save file
                // No need to check for this if not using world inventories - it is always the main file then
                if (MyWorlds.useWorldInventories && hasPlayedBefore && lastPlayerWorld != null) {
                    try {
                        // Allow switching worlds and positions
                        // Switch to the save file of the loaded world
                        files.setCurrentWorld(lastPlayerWorld);

                        if (!files.isMainWorld()) {
                            migrateSelfContainedMainProfile(files, mainWorldData);

                            // Load this world's specific player data
                            playerData = files.currentFile.read(player);
                            if (playerData == null) {
                                playerData = createEmptyData(player);
                            }

                            // Import legacy 'last player positions' of this world if we don't already have them
                            if (!lastPlayerPositions.containsWorld(files.currentFile.world)) {
                                CommonTagCompound pos = parseLegacyLastPosition(files.currentFile, playerData);
                                if (pos != null) {
                                    lastPlayerPositions.add(pos);
                                } else {
                                    lastPlayerPositions.addNoPositionSlot(files.currentFile.world);
                                }
                            }

                            // Preserve some of the global player state (stored in main world)
                            copyGlobalPlayerData(mainWorldData, playerData);
                        }
                    } catch (Throwable t) {
                        // Stick with the current world for now.
                        plugin.getLogger().log(Level.SEVERE, "Failed to load per-world-inventory player data of " + player.getName(), t);
                    }
                }

                // Initialize empty data for first-time joining
                if (playerData == null) {
                    playerData = createEmptyData(player);
                }

                // Migrate loaded player data to work on the current version of the server
                playerData = NBTUtil.migratePlayerProfileData(playerData);

                // Store a snapshot of this information for faster future retrieval
                storeLastPlayerPositions(player, lastPlayerPositions.clone());

                files.log("loading data");

                // When main world spawning is forced, reset location to there
                if (respawnAtServerSpawn) {
                    PlayerDataFile.setLocation(playerData, WorldManager.getSpawnLocation(MyWorlds.getMainWorld()));
                }

                // If for this world the inventory is cleared, clear relevant data in the NBT that should be removed
                World playerCurrentWorld = Bukkit.getWorld(((mainWorldData != null) ? mainWorldData : playerData).getUUID("World"));
                if (playerCurrentWorld != null && WorldConfig.get(playerCurrentWorld).clearInventory) {
                    resetPlayerData(playerData);

                    // Save this player data back to file to make sure clear inventory is adhered
                    files.currentFile.write(playerData);
                }

                // Disable bed spawn if not enabled for that world
                if (playerCurrentWorld != null) {
                    removeInvalidBedSpawn(playerCurrentWorld, playerData);
                }

                // Minecraft bugfix here: Clear mob/potion effects BEFORE loading the data
                // This resolves issues with effects staying behind
                resetCurrentMobEffects(player);

                // Load the entity using the player data compound
                NBTUtil.loadEntity(player, playerData);

                // Bukkit bug: entityplayer.e(tag) -> b(tag) -> craft.readExtraData(tag) which instantly sets it
                // Make sure the player is marked as being new
                PlayerUtil.setHasPlayedBefore(player, hasPlayedBefore);

                // As specified in the WorldNBTStorage implementation, use modified data if earlier than nbt data
                if (hasPlayedBefore && files.mainWorldFile.lastModified() < player.getFirstPlayed()) {
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
                                if (player.isValid()) {
                                    try {
                                        player.setFlying(true);
                                    } catch (IllegalArgumentException ex) {
                                    }
                                }
                            }
                        });
                    }
                }

                // Track whether this inventory was inventory edited
                if (loadedByInventoryEditor) {
                    InventoryEditRecovery.writeInventoryRecoveryData(files, mainWorldData, playerData);
                } else {
                    InventoryEditRecovery.clearEditedInventoryWorld(playerData);
                }

                return playerData;
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "Failed to load player data for " + player.getName(), t);
                return super.onLoad(player);
            }
        }
    }

    // Used on 1.20.5+ to handle when openinv loads player profiles 'offline'
    // Method callback added since BKCL 1.21.1 snapshots
    // @Override
    public CommonTagCompound onLoadOffline(String playerName, String playerUUID) {
        // There is no real way to tell, but this only gets called when openinv calls it
        // This is because the normal base operation of onLoad(player) calling this method doesn't happen,
        // as we override that method already.
        boolean loadedByInventoryEditor = true;

        // Always loaded...
        World vanillaMainWorld = WorldConfig.getVanillaMain().getWorld();;

        synchronized (PLAYER_PROFILE_LOCKS.getLock(playerUUID)) {
            try {
                final PlayerDataFileCollection files = new PlayerDataFileCollection(playerName, playerUUID, vanillaMainWorld);

                // Read the main world file first. We need this information regardless of whether or not
                // the MyWorlds inventories system is enabled. In here we store what world to send the player
                // to when multiple inventories are used. It also stores the last positions a player had on
                // various worlds.
                // If loading of this main world player profile fails, then we can't do anything more,
                // anyway.
                CommonTagCompound mainWorldData = files.mainWorldFile.readIfExists();
                if (mainWorldData == null) {
                    return null; // Player has not played before
                }

                CommonTagCompound playerData = mainWorldData; // Changed later if needed

                // If set to true, force player to respawn at the server spawn location as if joining
                // for the first time
                boolean respawnAtServerSpawn = false;

                // If force-joining the main world is enabled, and we got main world data, switch
                // the stored world to the MyWorlds main world
                if (MyWorlds.forceJoinOnMainWorld) {
                    mainWorldData.putUUID("World", MyWorlds.getMainWorld().getUID());
                    respawnAtServerSpawn = true;
                }

                // Check world player was last on actually still exists
                World lastPlayerWorld = Bukkit.getWorld(mainWorldData.getUUID("World"));
                if (lastPlayerWorld == null) {
                    respawnAtServerSpawn = true;
                }

                // Find out where to find the save file
                // No need to check for this if not using world inventories - it is always the main file then
                if (MyWorlds.useWorldInventories && lastPlayerWorld != null) {
                    try {
                        // Allow switching worlds and positions
                        // Switch to the save file of the loaded world
                        files.setCurrentWorld(lastPlayerWorld);

                        if (!files.isMainWorld()) {
                            migrateSelfContainedMainProfile(files, mainWorldData);

                            // Load this world's specific player data
                            playerData = files.currentFile.readIfExists();
                            if (playerData == null) {
                                playerData = PlayerDataFile.createEmptyData(playerUUID);
                            }

                            // Preserve some of the global player state (stored in main world)
                            copyGlobalPlayerData(mainWorldData, playerData);
                        }
                    } catch (Throwable t) {
                        // Stick with the current world for now.
                        plugin.getLogger().log(Level.SEVERE, "Failed to load per-world-inventory (offline) player data of " + playerName, t);
                    }
                }

                // Initialize empty data for first-time joining
                if (playerData == null) {
                    playerData = PlayerDataFile.createEmptyData(playerUUID);
                }

                // Migrate loaded player data to work on the current version of the server
                playerData = NBTUtil.migratePlayerProfileData(playerData);

                files.log("loading data");

                // When main world spawning is forced, reset location to there
                if (respawnAtServerSpawn) {
                    PlayerDataFile.setLocation(playerData, WorldManager.getSpawnLocation(MyWorlds.getMainWorld()));
                }

                // If for this world the inventory is cleared, clear relevant data in the NBT that should be removed
                World playerCurrentWorld = Bukkit.getWorld(((mainWorldData != null) ? mainWorldData : playerData).getUUID("World"));
                if (playerCurrentWorld != null && WorldConfig.get(playerCurrentWorld).clearInventory) {
                    resetPlayerData(playerData);

                    // Save this player data back to file to make sure clear inventory is adhered
                    files.currentFile.write(playerData);
                }

                // Disable bed spawn if not enabled for that world
                if (playerCurrentWorld != null) {
                    removeInvalidBedSpawn(playerCurrentWorld, playerData);
                }

                // Track whether this inventory was inventory edited
                InventoryEditRecovery.writeInventoryRecoveryData(files, mainWorldData, playerData);

                return playerData;
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "Failed to load (offline) player data for " + playerName, t);
                return null; //TODO: Super onloadoffline?
            }
        }
    }

    private Consumer<CommonTagCompound> saveCurrentPosition(final Player player, final Location loc, final boolean died) {
        // Track the last position on this world
        final LastPlayerPositionList lastPositions;
        synchronized (getLock(player)) {
            lastPositions = readLastPlayerPositions(player, Collections.emptyList()).clone();
            {
                LastPlayerPositionList.LastPosition pos = lastPositions.getForWorld(WorldConfig.get(loc.getWorld()));
                CommonTagCompound data = LastPlayerPositionList.createPositionData(loc, System.currentTimeMillis());
                if (died) {
                    data.putValue(LastPlayerPositionList.DATA_TAG_DIED, true);
                }
                lastPositions.update(pos, data);
            }
            storeLastPlayerPositions(player, lastPositions);
        }

        // This returned consumer will update the input tag compound and save the locations
        return data -> {
            // Update last positions
            CommonTagCompound myWorlds = data.createCompound(DATA_TAG_ROOT);
            myWorlds.put(DATA_TAG_LAST_POSITIONS, lastPositions.getDataTag());

            // Track for saved inventories whether at the time of saving multi-world inventories were enabled
            // This is important to detect pre-enabled inventory data and migrate those inventories appropriately.
            myWorlds.putValue(DATA_TAG_IS_SELF_CONTAINED, !MyWorlds.useWorldInventories);
        };
    }

    @Override
    public void onSave(final Player player) {
        synchronized (getLock(player)) {
            try {
                final PlayerDataFileCollection files;
                synchronized (worldToSaveTo) {
                    files = new PlayerDataFileCollection(player, worldToSaveTo.getOrDefault(player, player.getWorld()));
                }

                final CommonTagCompound savedData = NBTUtil.saveEntity(player, null);

                // Disable bed spawn if not enabled for that world
                removeInvalidBedSpawn(player.getWorld(), savedData);

                files.log("saving data");

                final Location loc = player.getLocation();
                if (files.isSingleDataFile()) {
                    // Append the Last Pos/Rot to the data
                    savedData.putListValues(LEGACY_DATA_TAG_LASTPOS, loc.getX(), loc.getY(), loc.getZ());
                    savedData.putListValues(LEGACY_DATA_TAG_LASTROT, loc.getYaw(), loc.getPitch());
                } else {
                    // Append original last position (if available) to the data
                    if (files.currentFile.exists()) {
                        CommonTagCompound data = files.currentFile.read(player);
                        if (data.containsKey(LEGACY_DATA_TAG_LASTPOS)) {
                            savedData.put(LEGACY_DATA_TAG_LASTPOS, data.get(LEGACY_DATA_TAG_LASTPOS));
                        }
                        if (data.containsKey(LEGACY_DATA_TAG_LASTROT)) {
                            savedData.put(LEGACY_DATA_TAG_LASTROT, data.get(LEGACY_DATA_TAG_LASTROT));
                        }
                    }

                    // Write the Last Pos/Rot to the official world file instead
                    files.positionFile.update(player, data -> {
                        data.putListValues(LEGACY_DATA_TAG_LASTPOS, loc.getX(), loc.getY(), loc.getZ());
                        data.putListValues(LEGACY_DATA_TAG_LASTROT, loc.getYaw(), loc.getPitch());

                        // Position file cannot possibly store this information
                        data.remove(LEGACY_DATA_TAG_LASTWORLD);

                        // Make sure position file is aware of the configured world inventories feature as well
                        // If we reach this, we know it's enabled
                        data.createCompound(DATA_TAG_ROOT).putValue(DATA_TAG_IS_SELF_CONTAINED, false);
                    });
                }

                // Track the last position on this world
                // Updates information tracked in the main world player data file
                final Consumer<CommonTagCompound> mainWorldUpdater = saveCurrentPosition(player, loc, player.isDead());

                // If main world, also write updated last position information to the file
                if (files.isMainWorld()) {
                    mainWorldUpdater.accept(savedData);
                }

                // Store last world player was on in the same file also storing inventory state
                // TODO: THIS IS LEGACY. Remove in the future! Is now part of the last positions info.
                savedData.putValue(LEGACY_DATA_TAG_LASTWORLD, loc.getWorld().getUID());

                // Save data to the destination file
                files.currentFile.write(savedData);

                // Write the current world name of the player to the save file of the main world
                if (!files.isMainWorld()) {
                    files.mainWorldFile.update(player, data -> {
                        data.put("Pos", savedData.get("Pos"));
                        data.put("Rotation", savedData.get("Rotation"));
                        data.putUUID("World", player.getWorld().getUID());
                        copyGlobalPlayerData(savedData, data);
                        mainWorldUpdater.accept(data);
                    });
                }
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "Failed to save player data for " + player.getName(), t);
            }
        }
    }

    /**
     * It's possible the player joined the server before MyWorlds split inventories were active.
     * This is the case when the main world player data file does not have the
     * DATA_TAG_LASTWORLD (MyWorlds.playerWorld) in the file, OR, when that player world actually
     * equals the world the player was last on.
     *
     * Basically, in such a situation, we check whether the main world player data file doesn't
     * actually store the inventory of the player on this other world.
     *
     * If this is the case, we make a 1:1 copy of the main world data at the location of this world.
     * We do this so that if some other plugins force the player to spawn on the main world/lobby,
     * the player's inventory data doesn't get lost.
     * We also WIPE the inventory state on the main world, to prevent the player having the same
     * inventory on both worlds.
     *
     * Beware: because of the legacy 'positionFile' behavior, where it writes the player's last
     * position on a particular world to file, it might be a mostly-empty player data file already
     * exists on that world. We must ignore that and trust the data of the main world, instead.
     *
     * @param files PlayerDataFileCollection
     * @param mainWorldData Previously loaded main profile data
     * @throws IOException
     */
    private static void migrateSelfContainedMainProfile(PlayerDataFileCollection files, CommonTagCompound mainWorldData) throws IOException {
        if (PlayerDataFile.isSelfContained(mainWorldData)) {
            // Copy main world profile -> world it is actually for
            StreamUtil.copyFile(files.mainWorldFile.file, files.currentFile.file);

            // Reset profile for main world. Track that it is now self-contained
            resetPlayerData(mainWorldData);
            mainWorldData.createCompound(DATA_TAG_ROOT).putValue(DATA_TAG_IS_SELF_CONTAINED, false);
            files.mainWorldFile.write(mainWorldData);
        }
    }

    private static void copyGlobalPlayerData(CommonTagCompound srcPlayerData, CommonTagCompound dstPlayerData) {
        // bukkit player stats
        {
            CommonTagCompound srcBukkit = srcPlayerData.get("bukkit", CommonTagCompound.class);
            if (srcBukkit != null) {
                CommonTagCompound dstBukkit = dstPlayerData.createCompound("bukkit");
                dstBukkit.put("lastPlayed", srcBukkit.get("lastPlayed"));
                dstBukkit.put("firstPlayed", srcBukkit.get("firstPlayed"));
                dstBukkit.put("lastKnownName", srcBukkit.get("lastKnownName"));
            }
        }

        // recipe book
        {
            CommonTagCompound srcRecipeBook = srcPlayerData.get("recipeBook", CommonTagCompound.class);
            if (srcRecipeBook != null) {
                dstPlayerData.put("recipeBook", srcRecipeBook);
            } else {
                dstPlayerData.remove("recipeBook");
            }
        }
    }

    private static void resetPlayerData(CommonTagCompound playerData) {
        // Create an empty CommonTagList with the right type information, by adding a compound and removing it
        // There is no good api in BKCommonLib to make an empty list of a given element type (TODO!)
        {
            CommonTagList emptyInventory = new CommonTagList();
            emptyInventory.add(new CommonTagCompound());
            emptyInventory.remove(0);
            playerData.put(VANILLA_INVENTORY_TAG, emptyInventory);
        }

        playerData.remove("attributes");
        playerData.remove("Attributes");
        playerData.remove("SelectedItemSlot");

        clearPotionEffects(playerData);
        playerData.putValue("XpLevel", 0);
        playerData.putValue("XpTotal", 0);
        playerData.putValue("XpP", 0.0F);
        playerData.remove("HealF");
        playerData.remove("Health");
    }

    private static CommonTagList readPotionEffects(CommonTagCompound data) {
        if (data.containsKey("ActiveEffects")) {
            return data.createList("ActiveEffects");
        } else if (data.containsKey("active_effects")) {
            return data.createList("active_effects");
        } else {
            return null;
        }
    }

    private static void clearPotionEffects(CommonTagCompound data) {
        data.remove("ActiveEffects");
        data.remove("active_effects");
    }

    private static void removeInvalidBedSpawn(World world, CommonTagCompound playerData) {
        PlayerRespawnPoint current = PlayerRespawnPoint.fromNBT(playerData);
        if (!current.isNone() && !WorldConfig.get(current.getWorld()).getBedRespawnMode().persistInProfile()) {
            PlayerRespawnPoint.NONE.toNBT(playerData);
        } else if (!isValidRespawnPoint(world, current)) {
            PlayerRespawnPoint.NONE.toNBT(playerData);
        }
    }
}
