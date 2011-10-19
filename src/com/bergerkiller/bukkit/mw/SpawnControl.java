package com.bergerkiller.bukkit.mw;

import java.util.HashSet;

import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Entity;

public class SpawnControl {
	
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
		
}
