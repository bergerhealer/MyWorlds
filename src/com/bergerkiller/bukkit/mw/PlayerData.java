package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.entity.CraftPlayer;

import com.bergerkiller.bukkit.common.reflection.SafeField;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

import net.minecraft.server.EntityHuman;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.IDataManager;
import net.minecraft.server.NBTCompressedStreamTools;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.PlayerFileData;
import net.minecraft.server.WorldNBTStorage;

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
	private Map<String, File> playerFileLoc = new HashMap<String, File>();

	public static void init() {
		CommonUtil.getServerConfig().playerFileData = new PlayerData();
	}

	@Override
	public String[] getSeenPlayers() {
		IDataManager man = WorldUtil.getWorlds().get(0).getDataManager();
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
			IDataManager man = WorldUtil.getNative(world).getDataManager();
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
		NBTCompressedStreamTools.a(nbttagcompound, new FileOutputStream(tmpDest));
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
			return NBTCompressedStreamTools.a(new FileInputStream(sourceFile));
		} else {
			NBTTagCompound empty = new NBTTagCompound();
			empty.setShort("Health", (short) 20);
			empty.setShort("HurtTime", (short) 0);
			empty.setShort("DeathTime", (short) 0);
			empty.setShort("AttackTime", (short) 0);
			empty.set("Motion", Util.doubleArrayToList(human.motX, human.motY, human.motZ));
			setLocation(empty, human.getBukkitEntity().getLocation());
			empty.setInt("Dimension", human.dimension);
			empty.setString("SpawnWorld", human.spawnWorld);
			return empty;
		}
	}

	private static void setLocation(NBTTagCompound nbttagcompound, Location location) {
		nbttagcompound.set("Pos", Util.doubleArrayToList(location.getX(), location.getY(), location.getZ()));
		nbttagcompound.set("Rotation", Util.floatArrayToList(location.getYaw(), location.getPitch()));
		UUID worldUUID = location.getWorld().getUID();
		nbttagcompound.setLong("WorldUUIDLeast", worldUUID.getLeastSignificantBits());
		nbttagcompound.setLong("WorldUUIDMost", worldUUID.getMostSignificantBits());
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
	}

	/**
	 * Applies the player states in the world to the player specified
	 * 
	 * @param world to get the states for
	 * @param player to set the states for
	 */
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
			player.inventory.b(data.getList("Inventory"));
			player.exp = data.getFloat("XpP");
			player.expLevel = data.getInt("XpLevel");
			player.expTotal = data.getInt("XpTotal");
			player.setHealth(data.getShort("Health"));
			player.getFoodData().a(data);
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
				main = playerFileLoc.get(entityhuman.name);
				if (main == null) {
					main = getMainFile(entityhuman.name);
					try {
						hasPlayedBefore = main.exists();
						if (hasPlayedBefore && !MyWorlds.forceMainWorldSpawn) {
							// Allow switching worlds and positions
							nbttagcompound = NBTCompressedStreamTools.a(new FileInputStream(main));
							long least = nbttagcompound.getLong("WorldUUIDLeast");
							long most = nbttagcompound.getLong("WorldUUIDMost");
							org.bukkit.World world = Bukkit.getWorld(new UUID(most, least));
							if (world != null) {
								main = getSaveFile(world.getName(), entityhuman.name);
							}
						}
					} catch (Exception exception) {
					}
					playerFileLoc.put(entityhuman.name, main);
				} else {
					hasPlayedBefore = true;
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
			entityhuman.e(nbttagcompound);
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
			NBTTagCompound nbttagcompound = new NBTTagCompound();
			entityhuman.d(nbttagcompound);
			File mainDest = getMainFile(entityhuman.name);
			File dest;
			if (MyWorlds.useWorldInventories) {
				// Use world specific save file
				dest = getSaveFile(entityhuman);
			} else {
				// Use main world save file
				dest = mainDest;
			}
			// Set the saved file location to quicken loading the next time
			playerFileLoc.put(entityhuman.name, dest);
			// Write to the source
			write(nbttagcompound, dest);
			if (mainDest.equals(dest)) {
				return; // Do not update world if same file
			}
			// Update the world in the main file
			if (mainDest.exists()) {
				nbttagcompound = NBTCompressedStreamTools.a(new FileInputStream(mainDest));
			}
			UUID worldUUID = entityhuman.world.getWorld().getUID();
			nbttagcompound.setLong("WorldUUIDLeast", worldUUID.getLeastSignificantBits());
			nbttagcompound.setLong("WorldUUIDMost", worldUUID.getMostSignificantBits());
			write(nbttagcompound, mainDest);
		} catch (Exception exception) {
			Bukkit.getLogger().warning("Failed to save player data for " + entityhuman.name);
			exception.printStackTrace();
		}
	}
}
