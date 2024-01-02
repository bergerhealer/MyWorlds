package com.bergerkiller.bukkit.mw.advancement;

import com.bergerkiller.bukkit.common.events.PlayerAdvancementProgressEvent;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.WorldConfig;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

class AdvancementManagerUsingProgressEvent implements AdvancementManager {
    private final MyWorlds plugin;
    private final boolean hasGameRuleEnum;

    public AdvancementManagerUsingProgressEvent(MyWorlds plugin) {
        this.plugin = plugin;
        this.hasGameRuleEnum = CommonUtil.getClass("org.bukkit.GameRule") != null;
    }

    @Override
    public void enable() {
        plugin.register(new Listener() {
            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onPlayerAdvancementProgress(PlayerAdvancementProgressEvent event) {
                if (!WorldConfig.get(event.getPlayer()).advancementsEnabled) {
                    event.setCancelled(true);
                }
            }
        });
    }

    @Override
    public void cacheAdvancements(Player player) {
    }

    @Override
    @SuppressWarnings("deprecation")
    public void applyGameRule(World world, boolean enabled) {
        if (this.hasGameRuleEnum) {
            applyGameRuleUsingEnum(world, enabled);
        } else {
            world.setGameRuleValue("announceAdvancements", enabled ? "true" : "false");
        }
    }

    private void applyGameRuleUsingEnum(World world, boolean enabled) {
        world.setGameRule(org.bukkit.GameRule.ANNOUNCE_ADVANCEMENTS, enabled);
    }
}
