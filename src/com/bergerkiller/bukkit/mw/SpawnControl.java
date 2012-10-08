package com.bergerkiller.bukkit.mw;

import java.util.HashSet;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import com.bergerkiller.bukkit.common.utils.EntityUtil;

public class SpawnControl {
	public HashSet<EntityType> deniedCreatures = new HashSet<EntityType>();

	public boolean isDenied(Entity entity) {
		return isDenied(entity.getType());
	}

	public boolean isDenied(EntityType type) {
		return type != null && deniedCreatures.contains(type);
	}

	public boolean isDenied(String type) {
		for (EntityType ctype : deniedCreatures) {
			if (ctype.name().equals(type)) {
				return true;
			}
		}
		if (type.endsWith("S")) {
			return isDenied(type.substring(0, type.length() - 2));
		}
		return false;
	}

	public void setAnimals(boolean deny) {
		for (EntityType type : EntityType.values()) {
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
		for (EntityType type : EntityType.values()) {
			if (EntityUtil.isMonster(type.toString().toLowerCase().replace("_", ""))) {
				if (deny) {
					deniedCreatures.add(type);
				} else {
					deniedCreatures.remove(type);
				}
			}
		}
	}

	public void setNPC(boolean deny) {
		for (EntityType type : EntityType.values()) {
			if (EntityUtil.isNPC(type.toString().toLowerCase().replace("_", ""))) {
				if (deny) {
					deniedCreatures.add(type);
				} else {
					deniedCreatures.remove(type);
				}
			}
		}
	}
}
