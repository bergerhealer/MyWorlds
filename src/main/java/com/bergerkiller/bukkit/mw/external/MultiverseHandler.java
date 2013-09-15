package com.bergerkiller.bukkit.mw.external;

import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import com.bergerkiller.bukkit.common.reflection.SafeField;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.Position;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldMode;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.WorldProperties;
import com.onarandombox.MultiverseCore.api.MVWorldManager;

/**
 * Deals with accessing Multiverse to take over the configuration for new worlds
 */
public class MultiverseHandler {

	public static boolean readWorldConfiguration(WorldConfig config) {
		if (MyWorlds.isMultiverseEnabled) {
			try {
				// Obtain Multiverse
				MultiverseCore core = CommonUtil.tryCast(CommonUtil.getPlugin("Multiverse-Core"), MultiverseCore.class);
				if (core == null) {
					MyWorlds.plugin.log(Level.WARNING, "Could not find Multiverse Core main plugin instance");
					return false;
				}

				// Obtain the world configuration information in MV
				MVWorldManager manager = core.getMVWorldManager();
				Map<String, WorldProperties> propsMap = SafeField.get(manager, "worldsFromTheConfig");
				WorldProperties world = propsMap.get(config.worldname);

				// Newly created world: no configuration in MV is available
				if (world == null) {
					MyWorlds.plugin.log(Level.WARNING, "World Configuration for '" + config.worldname + 
							"' could not be imported from Multiverse: No configuration available");
					return false;
				}

				// Serialization could be incomplete - this is a (hackish) check for that
				try {
					world.getDifficulty();
				} catch (NullPointerException ex) {
					return false;
				}

				// Apply general world settings
				config.difficulty = world.getDifficulty();
				config.allowHunger = world.getHunger();
				config.forcedRespawn = !world.getBedRespawn();
				config.holdWeather = !world.isWeatherEnabled();
				config.pvp = world.isPVPEnabled();
				config.gameMode = world.getGameMode();
				config.keepSpawnInMemory = world.isKeepingSpawnInMemory();
				config.worldmode = WorldMode.get(config.worldmode.getType(), world.getEnvironment());
				config.setChunkGeneratorName(world.getGenerator());

				// Apply the (re)spawn point
				String respawnWorldName = world.getRespawnToWorld();
				WorldProperties respawnWorld = propsMap.get(respawnWorldName);
				if (respawnWorld == null) {
					respawnWorld = world;
					respawnWorldName = config.worldname;
				}
				Location respawnLoc = respawnWorld.getSpawnLocation();
				if (respawnLoc != null) {
					config.spawnPoint = new Position(respawnLoc);
					if (respawnLoc.getWorld() == null) {
						config.spawnPoint.setWorldName(respawnWorldName);
					}
				}

				// Apply world animal/monster spawning rules
				config.spawnControl.deniedCreatures.clear();
				config.spawnControl.setAnimals(world.canAnimalsSpawn());
				config.spawnControl.setMonsters(world.canMonstersSpawn());
				for (EntityType type : EntityType.values()) {
					if (!EntityUtil.isAnimal(type) && !EntityUtil.isMonster(type)) {
						continue;
					}
					if (!world.getMonsterList().contains(type.getName()) && !world.getAnimalList().contains(type.getName())) {
						config.spawnControl.deniedCreatures.add(type);
					}
				}

				// Confirmation message and successful return
				MyWorlds.plugin.log(Level.WARNING, "World Configuration for '" + config.worldname + "' imported from Multiverse!");
				return true;
			} catch (Throwable t) {
				MyWorlds.plugin.getLogger().log(Level.SEVERE, "Failed to read World Configuration from Multiverse:", t);
			}
		}
		return false;
	}
}
