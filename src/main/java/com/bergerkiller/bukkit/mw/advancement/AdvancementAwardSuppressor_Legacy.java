package com.bergerkiller.bukkit.mw.advancement;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.mountiplex.reflection.ClassInterceptor;
import com.bergerkiller.mountiplex.reflection.resolver.Resolver;
import com.bergerkiller.mountiplex.reflection.util.FastField;
import com.bergerkiller.mountiplex.reflection.util.FastMethod;
import com.bergerkiller.mountiplex.reflection.util.fast.Invoker;
import org.bukkit.advancement.Advancement;

import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Only used on old versions of Minecraft (and old BKCommonLib versions).
 * Doesn't work on 1.20.2+ anymore because all of this stuff has become records.
 * Can be removed eventually when BKCL fully takes over.
 */
public class AdvancementAwardSuppressor_Legacy extends ClassInterceptor {
    private static final Class<?> craftAdvancementType = CommonUtil.getClass("org.bukkit.craftbukkit.advancement.CraftAdvancement");
    private static final FastMethod<Object> craftAdvancementGetHandle = new FastMethod<>();
    private static final FastMethod<Object> advancementHolderGetValue = new FastMethod<>();
    private static final FastField<Object> rewardsField = new FastField<>();
    private static Method grantMethod;

    static void init() {
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
            craftAdvancementGetHandle.initUnavailable("getHandle() method not found: " + t.getMessage());
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

    private Object advancement;
    private Object rewardsToRestore;

    private AdvancementAwardSuppressor_Legacy(Object advancement, Object rewardsToRestore) {
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

    static void suppressAdvancementRewards(Advancement advancement) {
        if (grantMethod == null || !craftAdvancementType.isInstance(advancement)) {
            return;
        }

        Object advHandle = craftAdvancementGetHandle.invoke(advancement);
        Object origRewards = rewardsField.get(advHandle);
        if (ClassInterceptor.get(origRewards, AdvancementAwardSuppressor_Legacy.class) == null) {
            // Not already hooked: hook it
            AdvancementAwardSuppressor_Legacy hook = new AdvancementAwardSuppressor_Legacy(advHandle, origRewards);
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
