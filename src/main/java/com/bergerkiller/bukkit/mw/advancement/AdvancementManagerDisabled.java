package com.bergerkiller.bukkit.mw.advancement;

import org.bukkit.World;
import org.bukkit.entity.Player;

class AdvancementManagerDisabled implements AdvancementManager {

    @Override
    public void enable() {
    }

    @Override
    public void notifyAdvancementsDisabledOnWorld() {
    }

    @Override
    public void cacheAdvancements(Player player) {
    }

    @Override
    public void applyGameRule(World world, boolean enabled) {
    }
}
