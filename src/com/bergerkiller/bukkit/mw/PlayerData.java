package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;

import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

import net.minecraft.server.EntityHuman;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.IDataManager;
import net.minecraft.server.NBTCompressedStreamTools;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.PlayerFileData;
import net.minecraft.server.World;
import net.minecraft.server.WorldNBTStorage;

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
	 * Gets the save file for the player in the current world
	 * 
	 * @param player to get the save file for
	 * @return save file
	 */
	public static File getSaveFile(EntityHuman player) {
		return getSaveFile(null, WorldConfig.get(player.world.getWorld()).inventory.getSharedWorldName(), player.name);
	}

	/**
	 * Gets the Main world save file for the playerName specified
	 * 
	 * @param playerName
	 * @return Save file
	 */
	public static File getMainFile(String playerName) {
		World world = WorldUtil.getWorlds().get(0);
		return getSaveFile(world, world.getWorld().getName(), playerName);
	}

	/**
	 * Gets the save file for the player in a world
	 * 
	 * @param worldname
	 * @return playername
	 */
	public static File getSaveFile(World world, String worldName, String playerName) {
		if (world == null) {
			org.bukkit.World bworld = Bukkit.getWorld(worldName);
			if (bworld != null) {
				world = WorldUtil.getNative(bworld);
			}
		}
		File playersFolder = null;
		if (world != null) {
			IDataManager man = world.getDataManager();
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
			empty.set("Pos", Util.doubleArrayToList(human.locX, human.locY, human.locZ));
			empty.set("Motion", Util.doubleArrayToList(human.motX, human.motY, human.motZ));
			empty.set("Rotation", Util.floatArrayToList(human.yaw, human.pitch));
			UUID worldUUID = human.world.getWorld().getUID();
			empty.setLong("WorldUUIDLeast", worldUUID.getLeastSignificantBits());
			empty.setLong("WorldUUIDMost", worldUUID.getMostSignificantBits());
			empty.setInt("Dimension", human.dimension);
			empty.setString("SpawnWorld", human.spawnWorld);
			return empty;
		}
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
		}
	}

	@Override
	public void load(EntityHuman entityhuman) {
		File main;
		// Get the source file to use for loading
		if (MyWorlds.useWorldInventories) {
			// Find out where to find the save file
			main = playerFileLoc.get(entityhuman.name);
			if (main == null) {
				main = getMainFile(entityhuman.name);
				try {
					if (main.exists()) {
						NBTTagCompound nbttagcompound = NBTCompressedStreamTools.a(new FileInputStream(main));
						long least = nbttagcompound.getLong("WorldUUIDLeast");
						long most = nbttagcompound.getLong("WorldUUIDMost");
						org.bukkit.World world = Bukkit.getWorld(new UUID(most, least));
						if (world != null) {
							main = getSaveFile(WorldUtil.getNative(world), world.getName(), entityhuman.name);
						}
					}
				} catch (Exception exception) {
				}
				playerFileLoc.put(entityhuman.name, main);
			}
		} else {
			// Just use the main world file
			main = getMainFile(entityhuman.name);
		}
		// Load the save file
		try {
			NBTTagCompound nbttagcompound = read(main, entityhuman);
			if (entityhuman instanceof EntityPlayer && main.exists()) {
				CraftPlayer player = (CraftPlayer) entityhuman.getBukkitEntity();
				player.setFirstPlayed(main.lastModified());
			}
			entityhuman.e(nbttagcompound);
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
		}
	}
}
