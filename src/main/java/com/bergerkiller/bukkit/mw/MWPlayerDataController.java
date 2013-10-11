package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.util.HashMap;
import java.util.zip.ZipException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.bases.IntVector3;
import com.bergerkiller.bukkit.common.controller.PlayerDataController;
import com.bergerkiller.bukkit.common.conversion.Conversion;
import com.bergerkiller.bukkit.common.entity.CommonEntity;
import com.bergerkiller.bukkit.common.entity.type.CommonLivingEntity;
import com.bergerkiller.bukkit.common.entity.type.CommonPlayer;
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
		File playerData =  WorldConfig.getMain().getPlayerData(playerName);
		playerData.getParentFile().mkdirs();
		return playerData;
	}

	/**
	 * Gets the World Configuration of the world player data is saved in.
	 * 
	 * @param player to get the save file world for
	 * @return save file world
	 */
	public static WorldConfig getSaveWorld(HumanEntity player) {
		if (MyWorlds.useWorldInventories) {
			return WorldConfig.get(WorldConfig.get(player).inventory.getSharedWorldName());
		} else {
			return WorldConfig.getMain();
		}
	}

	/**
	 * Gets the save file for the player in the current world.
	 * World inventories settings are applied here.
	 * 
	 * @param player to get the save file for
	 * @return save file
	 */
	public static File getSaveFile(HumanEntity player) {
		File playerData = getSaveWorld(player).getPlayerData(player.getName());
		playerData.getParentFile().mkdirs();
		return playerData;
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
	 * Creates new human data information as if the human just joined the server.
	 * 
	 * @param human to generate information about
	 * @return empty data
	 */
	public static CommonTagCompound createEmptyData(HumanEntity human) {
		final Vector velocity = human.getVelocity();
		CommonTagCompound empty = new CommonTagCompound();
		CommonLivingEntity<?> livingEntity = CommonEntity.get(human);
		empty.putValue("Health", (short) livingEntity.getMaxHealth());
		empty.putValue("HealF", (float) livingEntity.getMaxHealth()); // since 1.6.1 health is a float
		empty.putValue("HurtTime", (short) 0);
		empty.putValue("DeathTime", (short) 0);
		empty.putValue("AttackTime", (short) 0);
		empty.putListValues("Motion", velocity.getX(), velocity.getY(), velocity.getZ());
		setLocation(empty, WorldManager.getSpawnLocation(MyWorlds.getMainWorld()));
		final Object humanHandle = livingEntity.getHandle();
		IntVector3 coord = EntityHumanRef.spawnCoord.get(humanHandle);
		if (coord != null) {
			empty.putValue("SpawnWorld", EntityHumanRef.spawnWorld.get(humanHandle));
			empty.putValue("SpawnX", coord.x);
			empty.putValue("SpawnY", coord.y);
			empty.putValue("SpawnZ", coord.z);
		}
		return empty;
	}

	/**
	 * Tries to read the saved data from a source file
	 * 
	 * @param sourceFile to read from
	 * @return the data in the file, or the empty data constant if the file does not exist
	 * @throws Exception
	 */
	public static CommonTagCompound read(File sourceFile, HumanEntity human) {
		try {
			if (sourceFile.exists()) {
				return CommonTagCompound.readFrom(sourceFile);
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
	
	private static void clearEffects(HumanEntity human) {
		// Clear mob effects
		HashMap<Integer, Object> effects = EntityHumanRef.mobEffects.get(Conversion.toEntityHandle.convert(human));
		if (human instanceof Player) {
			// Send mob effect removal messages
			Player player = (Player) human;
			for (Object effect : effects.values()) {
				PacketUtil.sendPacket(player, PacketFields.REMOVE_MOB_EFFECT.newInstance(player.getEntityId(), effect));
			}
		}
		effects.clear();
		// Clear attributes
		MyWorlds.plugin.getAttributesUtil().resetAttributes(human);
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
			CommonPlayer commonPlayer = CommonEntity.get(player);
			Object playerHandle = Conversion.toEntityHandle.convert(player);
			File source = getSaveFile(player);
			CommonTagCompound data = read(source, player);

			// First, clear previous player information when loading involves adding new elements
			clearEffects(player);

			// Refresh attributes
			if (data.containsKey("Attributes")) {
				MyWorlds.plugin.getAttributesUtil().addAttributes(player, data.get("Attributes", CommonTagList.class));
			}

			// Load the data
			NBTUtil.loadInventory(player.getInventory(), data.createList("Inventory"));
			EntityHumanRef.exp.set(playerHandle, data.getValue("XpP", 0.0f));
			EntityHumanRef.expLevel.set(playerHandle, data.getValue("XpLevel", 0));
			EntityHumanRef.expTotal.set(playerHandle, data.getValue("XpTotal", 0));

			if (Common.MC_VERSION.equals("1.5.2")) {
				commonPlayer.setHealth(data.getValue("Health", (int) commonPlayer.getMaxHealth()));
			} else {
				commonPlayer.setHealth(data.getValue("HealF", (float) commonPlayer.getMaxHealth()));
			}
			
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
					try {
						// Allow switching worlds and positions
						tagcompound = CommonTagCompound.readFrom(main);
						World world = Bukkit.getWorld(tagcompound.getUUID("World"));
						if (world != null) {
							// Switch to the save file of the loaded world
							main = WorldConfig.get(world).getPlayerData(human.getName());
						}
					} catch (Throwable t) {
						// Stick with the current world for now.
					}
				}
			}
			tagcompound = read(main, human);
			if (!hasPlayedBefore || MyWorlds.forceMainWorldSpawn) {
				// Alter saved data to point to the main world
				setLocation(tagcompound, WorldManager.getSpawnLocation(MyWorlds.getMainWorld()));
			}

			// Minecraft bugfix here: Clear effects BEFORE loading the data
			// This resolves issues with effects staying behind
			clearEffects(human);

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

	/**
	 * Fired when a player respawns and all it's settings will be wiped.
	 * The player contains all information right before respawning.
	 * All data that would be wiped should be written has being wiped.
	 * This involves a manual save.
	 * 
	 * @param player that respawned
	 * @param respawnLocation where the player respawns at
	 */
	public void onRespawnSave(Player player, Location respawnLocation) {
		try {
			// Generate player saved information - used in favour of accessing NMS fields
			CommonTagCompound savedInfo = NBTUtil.saveEntity(player, null);
			// Generate a new tag compound with information
			CommonTagCompound tagcompound = createEmptyData(player);

			// We store this entire Bukkit tag + experience information!
			CommonTagCompound bukkitTag = savedInfo.get("bukkit", CommonTagCompound.class);
			if (bukkitTag != null) {
				// But, we do need to wipe information as specified
				if (bukkitTag.getValue("keepLevel", false)) {
					// Preserve experience
					bukkitTag.putValue("newTotalExp", savedInfo.getValue("XpTotal", 0));
					bukkitTag.putValue("newLevel", savedInfo.getValue("XpLevel", 0));
					tagcompound.putValue("XpP", savedInfo.getValue("XpP", 0.0f));
				}
				// Store experience (if not preserved, uses newTotal/newLevel) and the tag
				tagcompound.putValue("XpTotal", bukkitTag.getValue("newTotalExp", 0));
				tagcompound.putValue("XpLevel", bukkitTag.getValue("newLevel", 0));
				tagcompound.put("bukkit", bukkitTag);
			}

			// Ender inventory should not end up wiped!
			CommonTagList enderItems = savedInfo.get("EnderItems", CommonTagList.class);
			if (enderItems != null) {
				tagcompound.put("EnderItems", enderItems);
			}

			// Now, go ahead and save this data
			File mainDest = getMainFile(player.getName());
			File dest = getSaveFile(player);
			tagcompound.writeTo(dest);

			// Finally, we need to update where the player is at right now
			// To do so, we will write a new main world where the player is meant to be
			// This operation is a bit optional at this point, but it avoids possible issues in case of crashes
			// This is only needed if a main player data file doesn't exist
			// (this should in theory never happen either...player is not joining)
			if (mainDest.exists()) {
				tagcompound = CommonTagCompound.readFrom(mainDest);
				tagcompound.putUUID("World", respawnLocation.getWorld().getUID());
				tagcompound.writeTo(mainDest);
			}
		} catch (Exception exception) {
			Bukkit.getLogger().warning("Failed to save player respawned data for " + player.getName());
			exception.printStackTrace();
		}
	}

	@Override
	public void onSave(HumanEntity human) {
		try {
			CommonTagCompound tagcompound = NBTUtil.saveEntity(human, null);

			// Request several locations where player data is stored
			// Main file: the Main World folder where only the current World is stored
			// Pos file: the folder of the World the player is on where the position is stored			
			// Dest file: the inventory-merged folder where player info is stored
			File mainFile = getMainFile(human.getName());
			File posFile = WorldConfig.get(human).getPlayerData(human.getName());
			File destFile = getSaveFile(human);

			if (posFile.equals(destFile)) {
				// Saving data to the same world the player is on: simplified logic
				tagcompound.writeTo(destFile);
			} else {
				// Saving data to a different world than the player is on
				// Back up the Pos/Rotation in the dest file
				if (destFile.exists()) {
					CommonTagCompound oldData = CommonTagCompound.readFrom(destFile);
					tagcompound.put("Pos", oldData.get("Pos"));
					tagcompound.put("Rotation", oldData.get("Rotation"));
				} else {
					// No data found - assume the world spawn to be correct
					// This is not completely accurate, but will prevent possible bugs
					Position spawn = WorldManager.getSpawnPosition(getSaveWorld(human).worldname);
					tagcompound.putListValues("Pos", spawn.getX(), spawn.getY(), spawn.getZ());
					tagcompound.putListValues("Rotation", spawn.getYaw(), spawn.getPitch());
				}
				tagcompound.writeTo(destFile);
				
				// Write the Pos/Rot to the official world file instead
				// Load the data
				CommonTagCompound data = read(posFile, human);
				// Alter position information
				Location loc = human.getLocation();
				data.putListValues("Pos", loc.getX(), loc.getY(), loc.getZ());
				data.putListValues("Rotation", loc.getYaw(), loc.getPitch());
				// Save the updated data
				data.writeTo(posFile);
			}

			// Write the current world name of the player to the save file of the main world
			if (!mainFile.equals(destFile)) {
				// Update the world in the main file
				if (mainFile.exists()) {
					tagcompound = CommonTagCompound.readFrom(mainFile);
				}
				tagcompound.putUUID("World", human.getWorld().getUID());
				tagcompound.writeTo(mainFile);
			}
		} catch (Exception exception) {
			Bukkit.getLogger().warning("Failed to save player data for " + human.getName());
			exception.printStackTrace();
		}
	}
}
