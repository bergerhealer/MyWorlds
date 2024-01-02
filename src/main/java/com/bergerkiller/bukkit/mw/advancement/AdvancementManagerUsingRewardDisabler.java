package com.bergerkiller.bukkit.mw.advancement;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.events.PacketReceiveEvent;
import com.bergerkiller.bukkit.common.events.PacketSendEvent;
import com.bergerkiller.bukkit.common.protocol.PacketListener;
import com.bergerkiller.bukkit.common.protocol.PacketType;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.WorldConfig;

class AdvancementManagerUsingRewardDisabler implements AdvancementManager {
    private final MyWorlds plugin;
    private final Map<Player, Map<NamespacedKey, Collection<String>>> advancements;
    private final boolean hasGameRuleEnum;

    public AdvancementManagerUsingRewardDisabler(MyWorlds plugin) {
        this.plugin = plugin;
        this.advancements = new HashMap<>();
        this.hasGameRuleEnum = CommonUtil.getClass("org.bukkit.GameRule") != null;
    }

    @Override
    public void enable() {
        // Not available pre-1.12
        if (CommonUtil.getClass("org.bukkit.event.player.PlayerAdvancementDoneEvent", false) == null) {
            return;
        }

        // Ensure clinit initialized so we know of errors early on
        // Only needed when BKCommonLib lacks the API for this
        if (!Common.hasCapability("Common:Advancement:RewardDisabler")) {
            AdvancementAwardSuppressor_Legacy.init();
        }

        plugin.register(new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void onPlayerQuit(PlayerQuitEvent event) {
                advancements.remove(event.getPlayer());
            }

            @EventHandler(priority = EventPriority.MONITOR)
            public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
                if (WorldConfig.get(event.getPlayer()).advancementsEnabled) {
                    award(event.getPlayer(), event.getAdvancement());
                } else {
                    revoke(event.getPlayer(), event.getAdvancement());

                    // Also suppress rewards ever happening (disables grant(player))
                    if (Common.hasCapability("Common:Advancement:RewardDisabler")) {
                        disableNextGrant_BKCL(event.getAdvancement());
                    } else {
                        AdvancementAwardSuppressor_Legacy.suppressAdvancementRewards(event.getAdvancement());
                    }
                }
            }
        });

        // Disables the advancements packet when on a world where this is disabled
        plugin.register(new PacketListener() {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
            }

            @Override
            public void onPacketSend(PacketSendEvent event) {
                if (WorldConfig.get(event.getPlayer()).advancementsEnabled) {
                    return; // allow regardless
                }

                if (event.getPacket().read(PacketType.OUT_ADVANCEMENTS.initial)) {
                    return; // allow the initial list of advancements
                }

                // Deny any change/delta packets from being sent
                event.setCancelled(true);
            }
        }, PacketType.OUT_ADVANCEMENTS);
    }

    private static void disableNextGrant_BKCL(Advancement advancement) {
        CommonUtil.disableNextGrant(advancement);
    }

    @Override
    public void cacheAdvancements(Player player) {
        Iterator<Advancement> iter = Bukkit.advancementIterator();
        while (iter.hasNext()) {
            updateAdvancements(player, iter.next());
        }
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

    /**
     * Revokes criteria that were not already met before now
     * 
     * @param player
     * @param advancement
     */
    public void revoke(Player player, Advancement advancement) {
        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        Collection<String> awarded = getAdvancements(player).getOrDefault(advancement.getKey(), Collections.emptySet());
        for (String criteria : advancement.getCriteria()) {
            if (!awarded.contains(criteria)) {
                progress.revokeCriteria(criteria);
            }
        }
    }

    /**
     * Awards all the criteria of an advancement blindly
     * 
     * @param player
     * @param advancement
     */
    public void award(Player player, Advancement advancement) {
        getAdvancements(player).put(advancement.getKey(), advancement.getCriteria());
    }

    public void updateAdvancements(Player player, Advancement advancement) {
        getAdvancements(player).put(advancement.getKey(), player.getAdvancementProgress(advancement).getAwardedCriteria());
    }

    private Map<NamespacedKey, Collection<String>> getAdvancements(Player player) {
        return this.advancements.computeIfAbsent(player, (p) -> new HashMap<>());
    }
}
