package com.bergerkiller.bukkit.mw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.bukkit.World;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Entity;
import org.bukkit.util.config.Configuration;

public class SpawnControl {
	
	private static HashMap<String, SpawnRestriction> spawnRestrictions = new HashMap<String, SpawnRestriction>();
	public static class SpawnRestriction {
		public SpawnRestriction() {}
		public HashSet<CreatureType> deniedCreatures = new HashSet<CreatureType>();
		public boolean isDenied(Entity entity) {
			return isDenied(getCreature(entity));
		}
		public boolean isDenied(CreatureType type) {
			if (type == null) return false;
			return deniedCreatures.contains(type);
		}
		public boolean isDenied(String type) {
			for (CreatureType ctype : deniedCreatures) {
				if (ctype.name().equals(type)) return true;
			}
			if (type.endsWith("S")) {
				return isDenied(type.substring(0, type.length() - 2));
			}
			return false;
		}
		public void setAnimals(boolean deny) {
			for (CreatureType type : CreatureType.values()) {
				if (isAnimal(type)) {
					if (deny) {
						deniedCreatures.add(type);
					} else {
						deniedCreatures.remove(type);
					}
				}
			}
		}
		public void setMonsters(boolean deny) {
			for (CreatureType type : CreatureType.values()) {
				if (isMonster(type)) {
					if (deny) {
						deniedCreatures.add(type);
					} else {
						deniedCreatures.remove(type);
					}
				}
			}
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
	public static SpawnRestriction get(World w) {
		return get(w.getName());
	}
	
	public static boolean canSpawn(Entity entity) {
		return !get(entity.getWorld()).isDenied(entity);
	}
	
	public static boolean isMonster(CreatureType type) {
		if (type == null) return false;
		switch (type) {
		case CREEPER : return true;
		case GHAST : return true;
		case GIANT : return true;
		case MONSTER : return true;
		case PIG_ZOMBIE : return true;
		case SKELETON : return true;
		case SLIME : return true;
		case SPIDER : return true;
		case ZOMBIE : return true;
		case ENDERMAN : return true;
		case CAVE_SPIDER : return true;
		}
		return false;
	}
	public static boolean isAnimal(CreatureType type) {
		if (type == null) return false;
		switch (type) {
		case CHICKEN : return true;
		case COW : return true;
		case SHEEP : return true;
		case SQUID : return true;
		case WOLF : return true;
		case PIG : return true;
		}
		return false;
	}
	
	public static CreatureType getCreature(String name) {
		for (CreatureType ctype : CreatureType.values()) {
			if (name.equalsIgnoreCase(ctype.getName())) {
				return ctype;
			}
		}
		return null;
	}
	public static CreatureType getCreature(Entity e) {
		String name = e.getClass().getSimpleName();
		if (name.startsWith("Craft")) name = name.substring(5);
		return getCreature(name);
	}
	
	public static void load(Configuration config, String worldname) {
		SpawnRestriction r = get(worldname);
		for (String type : config.getStringList(worldname + ".deniedCreatures", new ArrayList<String>())) {
			type = type.toUpperCase();
			if (type.equals("ANIMALS")) {
				r.setAnimals(true);
			} else if (type.equals("MONSTERS")) {
				r.setMonsters(true);
			} else {
				try {
					r.deniedCreatures.add(CreatureType.valueOf(type));
				} catch (Exception ex) {}
			}
		}
	}
	public static void save(Configuration config) {
		for (Map.Entry<String, SpawnRestriction> restr : spawnRestrictions.entrySet()) {
			ArrayList<String> creatures = new ArrayList<String>();
			for (CreatureType type : restr.getValue().deniedCreatures) {
				creatures.add(type.name());
			}
			config.setProperty(restr.getKey() + ".deniedCreatures", creatures);
		}
	}
	
}
