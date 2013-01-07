package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_4_6.entity.CraftPlayer;

import com.bergerkiller.bukkit.common.reflection.SafeField;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.bukkit.common.utils.NBTUtil;
import com.bergerkiller.bukkit.common.utils.NativeUtil;

import net.minecraft.server.v1_4_6.ChunkCoordinates;
import net.minecraft.server.v1_4_6.EntityHuman;
import net.minecraft.server.v1_4_6.EntityPlayer;
import net.minecraft.server.v1_4_6.IDataManager;
import net.minecraft.server.v1_4_6.MobEffect;
import net.minecraft.server.v1_4_6.NBTTagCompound;
import net.minecraft.server.v1_4_6.NBTTagList;
import net.minecraft.server.v1_4_6.Packet41MobEffect;
import net.minecraft.server.v1_4_6.Packet42RemoveMobEffect;
import net.minecraft.server.v1_4_6.PlayerFileData;
import net.minecraft.server.v1_4_6.WorldNBTStorage;

/**
 * A player file data implementation that supports inventory sharing between worlds<br>
 * - The main world player data file contains the world the player joins in<br>
 * - The world defined by the inventory bundle contains all other data<br><br>
 * 
 * <b>When a player joins</b><br>
 * The main file is read to find out the save file. This save file is then read and 
 * applied on the player<br><br>
 * 
 * <b>When a player leaves</b><br>
 * The player data is written to the save file. If he was not on the main world, 
 * the main world file is updated with the current world the player is in<br><br>
 * 
 * <b>When a player teleports between worlds</b><br>
 * The old data is saved appropriately and the new data is applied again (not all data)
 */
public class PlayerData implements PlayerFileData {
	public static void init() {
		CommonUtil.getServerConfig().playerFileData = new PlayerData();
	}

	@Override
	public String[] getSeenPlayers() {
		IDataManager man = NativeUtil.getNative(MyWorlds.getMainWorld()).getDataManager();
		if (man instanceof WorldNBTStorage) {
			return ((WorldNBTStorage) man).getSeenPlayers();
		} else {
			return new String[0];
		}
	}

	/**
	 * Gets the Main world save file for the playerName specified
	 * 
	 * @param playerName
	 * @return Save file
	 */
	public static File getMainFile(String playerName) {
		World world = MyWorlds.getMainWorld();
		return getPlayerData(world.getName(), world, playerName);
	}

	/**
	 * Gets the save file for the player in the current world
	 * 
	 * @param player to get the save file for
	 * @return save file
	 */
	public static File getSaveFile(EntityHuman player) {
		return getSaveFile(player.world.getWorld().getName(), player.name);
	}

	/**
	 * Gets the save file for the player in a world
	 * 
	 * @param worldname
	 * @return playername
	 */
	public static File getSaveFile(String worldName, String playerName) {
		worldName = WorldConfig.get(worldName).inventory.getSharedWorldName();
		return getPlayerData(worldName, Bukkit.getWorld(worldName), playerName);
	}

	/**
	 * Gets the player data folder for a player in a certain world
	 * 
	 * @param worldName to use as backup
	 * @param world to use as main goal (can be null)
	 * @param playerName for the data
	 * @return Player data file
	 */
	private static File getPlayerData(String worldName, World world, String playerName) {
		File playersFolder = null;
		if (world != null) {
			IDataManager man = NativeUtil.getNative(world).getDataManager();
			if (man instanceof WorldNBTStorage) {
				playersFolder = ((WorldNBTStorage) man).getPlayerDir();
			}
		}
		if (playersFolder == null) {
			File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
			playersFolder = new File(worldFolder, "players");
		}
		return new File(playersFolder, playerName + ".dat");
	}

	/**
	 * Writes the compound to the destination file specified
	 * 
	 * @param nbttagcompound to save
	 * @param destFile to save to
	 * @throws Exception on any type of failure
	 */
	public static void write(NBTTagCompound nbttagcompound, File destFile) throws Exception {
		File tmpDest = new File(destFile.toString() + ".tmp");
		NBTUtil.writeCompound(nbttagcompound, new FileOutputStream(tmpDest));
		if (destFile.exists()) {
			destFile.delete();
		}
		tmpDest.renameTo(destFile);
	}

	/**
	 * Tries to read the saved data from a source file
	 * 
	 * @param sourceFile to read from
	 * @return the data in the file, or the empty data constant if the file does not exist
	 * @throws Exception
	 */
	public static NBTTagCompound read(File sourceFile, EntityHuman human) throws Exception {
		if (sourceFile.exists()) {
			return NBTUtil.readCompound(new FileInputStream(sourceFile));
		} else {
			NBTTagCompound empty = new NBTTagCompound();
			empty.setShort("Health", (short) 20);
			empty.setShort("HurtTime", (short) 0);
			empty.setShort("DeathTime", (short) 0);
			empty.setShort("AttackTime", (short) 0);
			empty.set("Motion", NBTUtil.doubleArrayToList(human.motX, human.motY, human.motZ));
			setLocation(empty, human.getBukkitEntity().getLocation());
			empty.setInt("Dimension", human.dimension);
			empty.setString("World", human.world.getWorld().getName());
			ChunkCoordinates coord = human.getBed();
			if (coord != null) {
				empty.setString("SpawnWorld", human.spawnWorld);
				empty.setInt("SpawnX", coord.x);
				empty.setInt("SpawnY", coord.y);
				empty.setInt("SpawnZ", coord.z);
			}
			return empty;
		}
	}

	private static void setLocation(NBTTagCompound nbttagcompound, Location location) {
		nbttagcompound.set("Pos", NBTUtil.doubleArrayToList(location.getX(), location.getY(), location.getZ()));
		nbttagcompound.set("Rotation", NBTUtil.floatArrayToList(location.getYaw(), location.getPitch()));
		World world = location.getWorld();
		nbttagcompound.setString("World", world.getName());
		NBTUtil.saveUUID(world.getUID(), nbttagcompound);
	}

	@SuppressWarnings("unchecked")
	private static void clearEffects(EntityHuman human) {
		// Send remove messages for all previous effects
		if (human instanceof EntityPlayer) {
			EntityPlayer ep = (EntityPlayer) human;
			if (ep.playerConnection != null) {
				for (MobEffect effect : (Collection<MobEffect>) human.effects.values()) {
					ep.playerConnection.sendPacket(new Packet42RemoveMobEffect(ep.id, effect));
				}
			}
		}
		human.effects.clear();
	}

	/**
	 * Handles post loading of an Entity
	 * 
	 * @param entityhuman that got loaded
	 */
	private static void postLoad(EntityHuman entityhuman) {
		if (WorldConfig.get(entityhuman.world.getWorld()).clearInventory) {
			Arrays.fill(entityhuman.inventory.items, null);
		}
		clearEffects(entityhuman);
	}

	/**
	 * Applies the player states in the world to the player specified
	 * 
	 * @param world to get the states for
	 * @param player to set the states for
	 */
	@SuppressWarnings("unchecked")
	public static void refreshState(EntityPlayer player) {
		if (!MyWorlds.useWorldInventories) {
			// If not enabled, only do the post-load logic
			postLoad(player);
			return;
		}
		try {
			File source = getSaveFile(player);
			NBTTagCompound data = read(source, player);
			// Load the inventory for that world
			NBTUtil.loadFromNBT(player.inventory, data.getList("Inventory"));
			player.exp = data.getFloat("XpP");
			player.expLevel = data.getInt("XpLevel");
			player.expTotal = data.getInt("XpTotal");
			player.setHealth(data.getShort("Health"));
			String spawnWorld = data.getString("SpawnWorld");
			boolean spawnForced = data.getBoolean("SpawnForced");
			if (LogicUtil.nullOrEmpty(spawnWorld)) {
				player.setRespawnPosition(null, spawnForced);
			} else if (data.hasKey("SpawnX") && data.hasKey("SpawnY") && data.hasKey("SpawnZ")) {
				player.setRespawnPosition(new ChunkCoordinates(data.getInt("SpawnX"), data.getInt("SpawnY"), data.getInt("SpawnZ")), spawnForced);
				player.spawnWorld = spawnWorld;
			}
			NBTUtil.loadFromNBT(player.getFoodData(), data);
			// Ender chest inventory
			NBTUtil.loadFromNBT(player.getEnderChest(), data.getList("EnderItems"));
			// Effects
			clearEffects(player);
			if (data.hasKey("ActiveEffects")) {
				NBTTagList nbttaglist = data.getList("ActiveEffects");
				for (int i = 0; i < nbttaglist.size(); ++i) {
					MobEffect mobeffect = NBTUtil.loadMobEffect((NBTTagCompound) nbttaglist.get(i));
					player.effects.put(Integer.valueOf(mobeffect.getEffectId()), mobeffect);
				}
			}
			// Send add messages for all new effects
			if (player instanceof EntityPlayer) {
				EntityPlayer ep = (EntityPlayer) player;
				for (MobEffect effect : (Collection<MobEffect>) player.effects.values()) {
					if (ep.playerConnection != null) {
						ep.playerConnection.sendPacket(new Packet41MobEffect(ep.id, effect));
					}
				}
			}
			player.updateEffects = true;
			postLoad(player);
		} catch (Exception exception) {
			Bukkit.getLogger().warning("Failed to load player data for " + player.name);
			exception.printStackTrace();
		}
	}

	@Override
	public void load(EntityHuman entityhuman) {
		try {
			File main;
			NBTTagCompound nbttagcompound;
			boolean hasPlayedBefore = false;
			// Get the source file to use for loading
			if (MyWorlds.useWorldInventories) {
				// Find out where to find the save file
				main = getMainFile(entityhuman.name);
				hasPlayedBefore = main.exists();
				if (hasPlayedBefore && !MyWorlds.forceMainWorldSpawn) {
					// Allow switching worlds and positions
					nbttagcompound = NBTUtil.readCompound(new FileInputStream(main));
					org.bukkit.World world = Bukkit.getWorld(NBTUtil.loadUUID(nbttagcompound));
					if (world != null) {
						// Switch to the save file of the loaded world
						main = getSaveFile(world.getName(), entityhuman.name);
					}
				}
			} else {
				// Just use the main world file
				main = getMainFile(entityhuman.name);
				hasPlayedBefore = main.exists();
			}
			nbttagcompound = read(main, entityhuman);
			if (!hasPlayedBefore || MyWorlds.forceMainWorldSpawn) {
				// Alter saved data to point to the main world
				setLocation(nbttagcompound, WorldManager.getSpawnLocation(MyWorlds.getMainWorld()));
			}
			// Load the save file
			NBTUtil.loadFromNBT(entityhuman, nbttagcompound);
			if (entityhuman instanceof EntityPlayer) {
				CraftPlayer player = (CraftPlayer) entityhuman.getBukkitEntity();
				if (hasPlayedBefore) {
					player.setFirstPlayed(main.lastModified());
				} else {
					// Bukkit bug: entityplayer.e(tag) -> b(tag) -> craft.readExtraData(tag) which instantly sets it
					// Make sure the player is marked as being new
					SafeField.set(player, "hasPlayedBefore", false);
				}
			}
			postLoad(entityhuman);
		} catch (Exception exception) {
			Bukkit.getLogger().warning("Failed to load player data for " + entityhuman.name);
			exception.printStackTrace();
		}
	}

	@Override
	public void save(EntityHuman entityhuman) {
		try {
			NBTTagCompound nbttagcompound = NBTUtil.saveToNBT(entityhuman);
			File mainDest = getMainFile(entityhuman.name);
			File dest;
			if (MyWorlds.useWorldInventories) {
				// Use world specific save file
				dest = getSaveFile(entityhuman);
			} else {
				// Use main world save file
				dest = mainDest;
			}
			// Write to the source
			write(nbttagcompound, dest);
			if (mainDest.equals(dest)) {
				return; // Do not update world if same file
			}
			// Update the world in the main file
			if (mainDest.exists()) {
				nbttagcompound = NBTUtil.readCompound(new FileInputStream(mainDest));
			}
			NBTUtil.saveUUID(entityhuman.world.getWorld().getUID(), nbttagcompound);
			write(nbttagcompound, mainDest);
		} catch (Exception exception) {
			Bukkit.getLogger().warning("Failed to save player data for " + entityhuman.name);
			exception.printStackTrace();
		}
	}
}
