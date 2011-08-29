package com.bergerkiller.bukkit.mw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Creature;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;

public class SpawnControl {
	
	private static HashMap<String, SpawnRestriction> spawnRestrictions = new HashMap<String, SpawnRestriction>();
	public static class SpawnRestriction {
		public SpawnRestriction() {}
		public HashSet<CreatureType> deniedCreatures = new HashSet<CreatureType>();
		public boolean denyMonsters = false;
		public boolean denyAnimals = false;
		public boolean isDenied(Entity e, CreatureType type) {
			if (e instanceof Animals) {
				if (denyAnimals) return true;
			} else if (e instanceof Monster) {
				if (denyMonsters) return true;
			}
			return deniedCreatures.contains(type);
		}		
		public boolean isDenied(String type) {
			type = type.toUpperCase();
			if (type.equalsIgnoreCase("ANIMAL")) {
				return denyAnimals;
			} else if (type.equalsIgnoreCase("MONSTER")) {
				return denyMonsters;
			} else {
				for (CreatureType ctype : deniedCreatures) {
					if (ctype.name().equals(type)) return true;
				}
			}
			if (type.endsWith("S")) {
				return isDenied(type.substring(0, type.length() - 2));
			}
			return false;
		}
	}
	
	public static SpawnRestriction get(String worldname) {
		worldname = worldname.toLowerCase();
		SpawnRestriction restr = spawnRestrictions.get(worldname);
		if (restr == null) {
			restr = new SpawnRestriction();
			spawnRestrictions.put(worldname, restr);
		}
		return restr;
	}
	
	public static boolean canSpawn(World w, Entity e, CreatureType type) {
		return !get(w.getName()).isDenied(e, type);
	}

	public static void load(String filename) {
		SafeReader reader = new SafeReader(filename);
		while (true) {
			String worldname = reader.readNonEmptyLine();
			if (worldname == null) break;
			//Get the denied types for this world
			SpawnRestriction r = get(worldname);
			while (true) {
				String typeline = reader.readNonEmptyLine();
				if (typeline == null) break;
				typeline = typeline.trim().toUpperCase();
				if (typeline.equals("ANIMALS")) {
					r.denyAnimals = true;
				} else if (typeline.equals("MONSTERS")) {
					r.denyMonsters = true;
				} else {
					CreatureType type = CreatureType.valueOf(typeline);
					if (type != null) {
						r.deniedCreatures.add(type);
					} else {
						break;
					}
				}
			}
		}
		reader.close();
	}
	public static void save(String filename) {
		SafeWriter writer = new SafeWriter(filename);
		for (String worldname : spawnRestrictions.keySet()) {
			SpawnRestriction restr = spawnRestrictions.get(worldname);
			if (restr == null) continue;
			writer.writeLine(worldname);
			if (restr.denyAnimals) writer.writeLine("    ANIMALS");
			if (restr.denyMonsters) writer.writeLine("    MONSTERS");
			for (CreatureType type : restr.deniedCreatures) {
				writer.writeLine("    " + type.name());
			}
			writer.writeLine("END");
		}
		writer.close();
	}
	
}
