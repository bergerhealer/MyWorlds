package com.bergerkiller.bukkit.mw;

import java.util.HashSet;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.EntityGroupingUtil.EntityCategory;

public class SpawnControl {
    public HashSet<EntityType> deniedCreatures = new HashSet<EntityType>();

    public boolean isDenied(Entity entity) {
        // Preserve entities that have a nametag!
        if (entity.getCustomName() != null) {
            return false;
        }

        return isDenied(entity.getType());
    }

    public boolean isDenied(EntityType type) {
        return deniedCreatures.contains(type);
    }

    public boolean getAnimals() {
        for (EntityType type : EntityType.values()) {
            if (EntityUtil.isEntityType(type, EntityCategory.ANIMAL)) {
                if (!this.deniedCreatures.contains(type)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean getMonsters() {
        for (EntityType type : EntityType.values()) {
            if (EntityUtil.isEntityType(type, EntityCategory.MONSTER)) {
                if (!this.deniedCreatures.contains(type)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void setAnimals(boolean deny) {
        for (EntityType type : EntityType.values()) {
            if (EntityUtil.isEntityType(type, EntityCategory.ANIMAL)) {
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
            if (EntityUtil.isEntityType(type, EntityCategory.MONSTER)) {
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
            if (EntityUtil.isEntityType(type, EntityCategory.NPC)) {
                if (deny) {
                    deniedCreatures.add(type);
                } else {
                    deniedCreatures.remove(type);
                }
            }
        }
    }
}
