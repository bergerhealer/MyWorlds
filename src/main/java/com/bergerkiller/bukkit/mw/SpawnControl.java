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
		return deniedCreatures.contains(type);
	}

	public boolean getAnimals() {
		for (EntityType type : EntityType.values()) {
			if (EntityUtil.isAnimal(type)) {
				if (!this.deniedCreatures.contains(type)) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean getMonsters() {
		for (EntityType type : EntityType.values()) {
			if (EntityUtil.isMonster(type)) {
				if (!this.deniedCreatures.contains(type)) {
					return false;
				}
			}
		}
		return true;
	}

	public void setAnimals(boolean deny) {
		for (EntityType type : EntityType.values()) {
			if (EntityUtil.isAnimal(type)) {
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
			if (EntityUtil.isMonster(type)) {
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
			if (EntityUtil.isNPC(type)) {
				if (deny) {
					deniedCreatures.add(type);
				} else {
					deniedCreatures.remove(type);
				}
			}
		}
	}
}
