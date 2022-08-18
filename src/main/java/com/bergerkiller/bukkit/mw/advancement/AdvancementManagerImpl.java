package com.bergerkiller.bukkit.mw.advancement;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

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
import com.bergerkiller.mountiplex.reflection.ClassInterceptor;
import com.bergerkiller.mountiplex.reflection.resolver.Resolver;
import com.bergerkiller.mountiplex.reflection.util.FastField;
import com.bergerkiller.mountiplex.reflection.util.FastMethod;
import com.bergerkiller.mountiplex.reflection.util.fast.Invoker;

public class AdvancementManagerImpl implements AdvancementManager {
    private final MyWorlds plugin;
    private final Map<Player, Map<NamespacedKey, Collection<String>>> advancements;
    private final boolean hasGameRuleEnum;

    public AdvancementManagerImpl(MyWorlds plugin) {
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
        AdvancementAwardSuppressor.init();

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
                    AdvancementAwardSuppressor.suppressAdvancementRewards(event.getAdvancement());
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

    /**
     * Replaces the normal awards instance temporarily, re-implementing grant() so that no
     * action follows.
     */
    public static class AdvancementAwardSuppressor extends ClassInterceptor {
        private static final Class<?> craftAdvancementType = CommonUtil.getClass("org.bukkit.craftbukkit.advancement.CraftAdvancement");
        private static final FastMethod<Object> craftAdvancementGetHandle = new FastMethod<>();
        private static final FastField<Object> rewardsField = new FastField<>();
        private static final Method grantMethod;
        static {
            Class<?> rewardsType = CommonUtil.getClass("net.minecraft.advancements.AdvancementRewards");
            try {
                Class<?> advancementType = CommonUtil.getClass("net.minecraft.advancements.Advancement");
                if (Common.evaluateMCVersion(">=", "1.15.2")) {
                    rewardsField.init(Resolver.resolveAndGetDeclaredField(advancementType, "rewards"));
                } else {
                    rewardsField.init(Resolver.resolveAndGetDeclaredField(advancementType, "c"));
                }
                if (rewardsField.getType() != rewardsType) {
                    rewardsField.initUnavailable("Rewards field not found: invalid field type");
                    MyWorlds.plugin.getLogger().log(Level.SEVERE, "Failed to find advancements rewards field: " +
                          "type is " + rewardsField.getType() + " instead of " + rewardsType);
                }
            } catch (Throwable t) {
                rewardsField.initUnavailable("Rewards field not found: " + t.getMessage());
                MyWorlds.plugin.getLogger().log(Level.SEVERE, "Failed to find advancements rewards field", t);
            }
            try {
                craftAdvancementGetHandle.init(craftAdvancementType.getDeclaredMethod("getHandle"));
            } catch (Throwable t) {
                rewardsField.initUnavailable("getHandle() method not found: " + t.getMessage());
                MyWorlds.plugin.getLogger().log(Level.SEVERE, "Failed to find advancement getHandle() method", t);
            }
            {
                Method m;
                try {
                    Class<?> epType = CommonUtil.getClass("net.minecraft.server.level.EntityPlayer");
                    if (Common.evaluateMCVersion(">=", "1.18")) {
                        m = Resolver.resolveAndGetDeclaredMethod(rewardsType, "grant", epType);
                    } else {
                        m = Resolver.resolveAndGetDeclaredMethod(rewardsType, "a", epType);
                    }
                } catch (Throwable t) {
                    m = null;
                    MyWorlds.plugin.getLogger().log(Level.SEVERE, "Grant method not found in AdvancementRewards", t);
                }
                grantMethod = m;
            }
        }

        public static void init() {
            // Dummy
        }

        private Object advancement;
        private Object rewardsToRestore;

        private AdvancementAwardSuppressor(Object advancement, Object rewardsToRestore) {
            this.advancement = advancement;
            this.rewardsToRestore = rewardsToRestore;
        }

        public void restore() {
            if (advancement != null && rewardsToRestore != null) {
                rewardsField.set(advancement, rewardsToRestore);
                advancement = null;
                rewardsToRestore = null;
            }
        }

        private static void suppressAdvancementRewards(Advancement advancement) {
            if (grantMethod == null || !craftAdvancementType.isInstance(advancement)) {
                return;
            }

            Object advHandle = craftAdvancementGetHandle.invoke(advancement);
            Object origRewards = rewardsField.get(advHandle);
            if (ClassInterceptor.get(origRewards, AdvancementAwardSuppressor.class) == null) {
                // Not already hooked: hook it
                AdvancementAwardSuppressor hook = new AdvancementAwardSuppressor(advHandle, origRewards);
                rewardsField.set(advHandle, hook.hook(origRewards));
                CommonUtil.nextTick(() -> hook.restore());
            }
        }

        @Override
        protected Invoker<?> getCallback(Method method) {
            if (method.equals(grantMethod)) {
                return (instance, args) -> {
                    restore();
                    return null;
                };
            }
            return null;
        }
    }
}
