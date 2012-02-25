package com.bergerkiller.bukkit.mw;

import java.util.HashSet;

import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Entity;

import com.bergerkiller.bukkit.common.utils.EntityUtil;

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
			if (EntityUtil.isAnimal(type.toString().toLowerCase().replace("_", ""))) {
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
			if (EntityUtil.isMonster(type.toString().toLowerCase().replace("_", ""))) {
				if (deny) {
					deniedCreatures.add(type);
				} else {
					deniedCreatures.remove(type);
				}
			}
		}
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
