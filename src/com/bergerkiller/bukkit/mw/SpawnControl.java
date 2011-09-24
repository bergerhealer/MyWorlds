package com.bergerkiller.bukkit.mw;

import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.World;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Entity;

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
					r.setAnimals(true);
				} else if (typeline.equals("MONSTERS")) {
					r.setMonsters(true);
				} else {
					CreatureType type = null;
					try {
						type = CreatureType.valueOf(typeline);
					} catch (Exception ex) {}
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
			if (restr.deniedCreatures.size() == 0) continue;
			writer.writeLine(worldname);
			for (CreatureType type : restr.deniedCreatures) {
				writer.writeLine("    " + type.name());
			}
			writer.writeLine("END");
		}
		writer.close();
	}
	
}
