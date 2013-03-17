package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.bergerkiller.bukkit.common.bases.IntVector3;
import com.bergerkiller.bukkit.common.controller.PlayerDataController;
import com.bergerkiller.bukkit.common.conversion.Conversion;
import com.bergerkiller.bukkit.common.nbt.CommonTagCompound;
import com.bergerkiller.bukkit.common.nbt.CommonTagList;
import com.bergerkiller.bukkit.common.protocol.PacketFields;
import com.bergerkiller.bukkit.common.reflection.classes.EntityHumanRef;
import com.bergerkiller.bukkit.common.reflection.classes.MobEffectRef;
import com.bergerkiller.bukkit.common.utils.NBTUtil;
import com.bergerkiller.bukkit.common.utils.PacketUtil;
import com.bergerkiller.bukkit.common.utils.PlayerUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

public class MWPlayerDataController extends PlayerDataController {

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
	public static File getSaveFile(HumanEntity player) {
		return getSaveFile(player.getWorld().getName(), player.getName());
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
		final File playersFolder;
		if (world == null) {
			playersFolder = new File(WorldUtil.getWorldFolder(worldName), "players");
		} else {
			playersFolder = WorldUtil.getPlayersFolder(world);
		}
		return new File(playersFolder, playerName + ".dat");
	}

	private static void setLocation(CommonTagCompound tagCompound, Location location) {
		tagCompound.putListValues("Pos", location.getX(), location.getY(), location.getZ());
		tagCompound.putListValues("Rotation", location.getYaw(), location.getPitch());
		final World world = location.getWorld();
		tagCompound.putValue("World", world.getName());
		tagCompound.putUUID("World", world.getUID());
		tagCompound.putValue("Dimension", WorldUtil.getDimension(world));
	}

	/**
	 * Tries to read the saved data from a source file
	 * 
	 * @param sourceFile to read from
	 * @return the data in the file, or the empty data constant if the file does not exist
	 * @throws Exception
	 */
	public static CommonTagCompound read(File sourceFile, HumanEntity human) throws Exception {
		if (sourceFile.exists()) {
			return CommonTagCompound.readFrom(sourceFile);
		} else {
			final Vector velocity = human.getVelocity();
			CommonTagCompound empty = new CommonTagCompound();
			empty.putValue("Health", (short) 20);
			empty.putValue("HurtTime", (short) 0);
			empty.putValue("DeathTime", (short) 0);
			empty.putValue("AttackTime", (short) 0);
			empty.putListValues("Motion", velocity.getX(), velocity.getY(), velocity.getZ());
			setLocation(empty, human.getLocation());
			final Object humanHandle = Conversion.toEntityHandle.convert(human);
			IntVector3 coord = EntityHumanRef.spawnCoord.get(humanHandle);
			if (coord != null) {
				empty.putValue("SpawnWorld", EntityHumanRef.spawnWorld.get(humanHandle));
				empty.putValue("SpawnX", coord.x);
				empty.putValue("SpawnY", coord.y);
				empty.putValue("SpawnZ", coord.z);
			}
			return empty;
		}
	}

	private static void clearEffects(HumanEntity human) {
		HashMap<Integer, Object> effects = EntityHumanRef.mobEffects.get(Conversion.toEntityHandle.convert(human));
		if (human instanceof Player) {
			// Send mob effect removal messages
			Player player = (Player) human;
			for (Object effect : effects.values()) {
				PacketUtil.sendPacket(player, PacketFields.REMOVE_MOB_EFFECT.newInstance(player.getEntityId(), effect));
			}
		}
		effects.clear();
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
			Object playerHandle = Conversion.toEntityHandle.convert(player);
			File source = getSaveFile(player);
			CommonTagCompound data = read(source, player);

			// Load the data
			NBTUtil.loadInventory(player.getInventory(), data.createList("Inventory"));
			EntityHumanRef.exp.set(playerHandle, data.getValue("XpP", 0.0f));
			EntityHumanRef.expLevel.set(playerHandle, data.getValue("XpLevel", 0));
			EntityHumanRef.expTotal.set(playerHandle, data.getValue("XpTotal", 0));
			player.setHealth(data.getValue("Health", player.getMaxHealth()));

			// Respawn position
			String spawnWorld = data.getValue("SpawnWorld", "");
			IntVector3 spawn = null;
			if (!spawnWorld.isEmpty()) {
				Integer x = data.getValue("SpawnX", Integer.class);
				Integer y = data.getValue("SpawnY", Integer.class);
				Integer z = data.getValue("SpawnZ", Integer.class);
				if (x != null && y != null && z != null) {
					spawn = new IntVector3(x, y, z);
				} else {
					spawnWorld = ""; //reset, invalid coordinates
				}
			}
			EntityHumanRef.spawnCoord.set(playerHandle, spawn);
			EntityHumanRef.spawnWorld.set(playerHandle, spawnWorld);
			EntityHumanRef.spawnForced.set(playerHandle, data.getValue("SpawnForced", false));

			// Other data
			NBTUtil.loadFoodMetaData(EntityHumanRef.foodData.get(playerHandle), data);
			NBTUtil.loadInventory(player.getEnderChest(), data.createList("EnderItems"));

			// Load Mob Effects
			clearEffects(player);
			HashMap<Integer, Object> effects = EntityHumanRef.mobEffects.get(playerHandle);
			if (data.containsKey("ActiveEffects")) {
				CommonTagList taglist = data.createList("ActiveEffects");
				for (int i = 0; i < taglist.size(); ++i) {
					Object mobEffect = NBTUtil.loadMobEffect((CommonTagCompound) taglist.get(i));
					effects.put(MobEffectRef.effectId.get(mobEffect), mobEffect);
				}
			}
			EntityHumanRef.updateEffects.set(playerHandle, true);

			// Send add messages for all (new) effects
			for (Object effect : effects.values()) {
				PacketUtil.sendPacket(player, PacketFields.MOB_EFFECT.newInstance(player.getEntityId(), effect));
			}

			// Perform post loading
			postLoad(player);
		} catch (Exception exception) {
			Bukkit.getLogger().warning("Failed to load player data for " + player.getName());
			exception.printStackTrace();
		}
	}

	@Override
	public CommonTagCompound onLoad(HumanEntity human) {
		try {
			File main;
			CommonTagCompound tagcompound;
			boolean hasPlayedBefore = false;
			// Get the source file to use for loading
			main = getMainFile(human.getName());
			hasPlayedBefore = main.exists();
			if (MyWorlds.useWorldInventories) {
				// Find out where to find the save file
				if (hasPlayedBefore && !MyWorlds.forceMainWorldSpawn) {
					// Allow switching worlds and positions
					tagcompound = CommonTagCompound.readFrom(main);
					World world = Bukkit.getWorld(tagcompound.getUUID("World"));
					if (world != null) {
						// Switch to the save file of the loaded world
						main = getSaveFile(world.getName(), human.getName());
					}
				}
			}
			tagcompound = read(main, human);
			if (!hasPlayedBefore || MyWorlds.forceMainWorldSpawn) {
				// Alter saved data to point to the main world
				setLocation(tagcompound, WorldManager.getSpawnLocation(MyWorlds.getMainWorld()));
			}

			// Load the save file
			NBTUtil.loadEntity(human, tagcompound);
			if (human instanceof Player) {
				// Bukkit bug: entityplayer.e(tag) -> b(tag) -> craft.readExtraData(tag) which instantly sets it
				// Make sure the player is marked as being new
				PlayerUtil.setHasPlayedBefore((Player) human, hasPlayedBefore);
				if (hasPlayedBefore) {
					// As specified in the WorldNBTStorage implementation, set this
					PlayerUtil.setFirstPlayed((Player) human, main.lastModified());
				}
			}
			postLoad(human);
			return tagcompound;
		} catch (Exception exception) {
			Bukkit.getLogger().warning("Failed to load player data for " + human.getName());
			exception.printStackTrace();
			return super.onLoad(human);
		}
	}

	@Override
	public void onSave(HumanEntity human) {
		try {
			CommonTagCompound tagcompound = NBTUtil.saveEntity(human, null);
			File mainDest = getMainFile(human.getName());
			File dest;
			if (MyWorlds.useWorldInventories) {
				// Use world specific save file
				dest = getSaveFile(human);
			} else {
				// Use main world save file
				dest = mainDest;
			}
			// Write to the source
			tagcompound.writeTo(dest);
			if (mainDest.equals(dest)) {
				return; // Do not update world if same file
			}
			// Update the world in the main file
			if (mainDest.exists()) {
				tagcompound = CommonTagCompound.readFrom(mainDest);
			}
			tagcompound.putUUID("World", human.getWorld().getUID());
			tagcompound.writeTo(mainDest);
		} catch (Exception exception) {
			Bukkit.getLogger().warning("Failed to save player data for " + human.getName());
			exception.printStackTrace();
		}
	}
}
