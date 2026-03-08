package com.bergerkiller.bukkit.mw.advancement;

import com.bergerkiller.bukkit.common.events.PlayerAdvancementProgressEvent;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.utils.GameRuleWrapper;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

class AdvancementManagerUsingProgressEvent implements AdvancementManager {
    private final MyWorlds plugin;
    private final GameRuleWrapper<Boolean> showAnnouncementsGameRule;
    private boolean listenerRegistered = false;

    public AdvancementManagerUsingProgressEvent(MyWorlds plugin) {
        this.plugin = plugin;
        this.showAnnouncementsGameRule = GameRuleWrapper.forBoolean("announceAdvancements", "show_advancement_messages");
    }

    @Override
    public void enable() {
        PlayerAdvancementProgressEvent.getHandlerList(); // Fail if this class doesn't exist or doesn't work
    }

    @Override
    public void notifyAdvancementsDisabledOnWorld() {
        if (!listenerRegistered) {
            listenerRegistered = true;
            plugin.register(new Listener() {
                @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
                public void onPlayerAdvancementProgress(PlayerAdvancementProgressEvent event) {
                    if (!WorldConfig.get(event.getPlayer()).isAdvancementsEnabled()) {
                        event.setCancelled(true);
                    }
                }
            });
        }
    }

    @Override
    public void cacheAdvancements(Player player) {
    }

    @Override
    public void applyGameRule(World world, boolean enabled) {
        this.showAnnouncementsGameRule.set(world, enabled);
    }
}
