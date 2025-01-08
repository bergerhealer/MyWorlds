package com.bergerkiller.bukkit.mw.portal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.component.LibraryComponent;
import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.mw.MyWorlds;

/**
 * For some entities, a Portal Enter Event fires multiple times in a single
 * tick. This class debounces those events so only one event is fired. This
 * also makes sure that entities aren't in the middle of physics calculations
 * where Entity Location could be invalid.
 */
public final class PortalEnterEventDebouncer implements LibraryComponent {
    private final Set<Pending> pending = new LinkedHashSet<>();
    private final Set<Pending> runThisTick = new HashSet<>();
    private final Callback callback;
    private final Task task;

    public PortalEnterEventDebouncer(MyWorlds plugin, Callback callback) {
        this.callback = callback;
        this.task = new Task(plugin) {
            @Override
            public void run() {
                if (!pending.isEmpty()) {
                    ArrayList<Pending> copy = new ArrayList<>(pending);
                    pending.clear();
                    for (Pending pending : copy) {
                        if (!runThisTick.contains(pending)) {
                            callback.onPortalEnter(pending.portalBlock, pending.entity, pending.portalCooldown);
                        }
                    }
                }

                // Note: it's possible this contains items and pending is empty when clear(entity) was called
                runThisTick.clear();
            }
        };
    }

    @Override
    public void enable() {
        task.start(1, 1);
    }

    @Override
    public void disable() {
        pending.clear();
        runThisTick.clear();
        task.stop();
    }

    public void triggerAndRunOnceATick(Block portalBlock, Entity entity) {
        Pending newPending = new Pending(portalBlock, entity);
        pending.add(newPending);
        if (runThisTick.add(newPending)) {
            callback.onPortalEnter(newPending.portalBlock, newPending.entity, newPending.portalCooldown);
        }
    }

    public void trigger(Block portalBlock, Entity entity) {
        pending.add(new Pending(portalBlock, entity));
    }

    public void clear(Entity entity) {
        for (Iterator<Pending> iter = pending.iterator(); iter.hasNext();) {
            if (iter.next().entity == entity) {
                iter.remove();
            }
        }
    }

    private static final class Pending {
        public final Block portalBlock;
        public final Entity entity;
        public final int portalCooldown;

        public Pending(Block portalBlock, Entity entity) {
            this.portalBlock = portalBlock;
            this.entity = entity;
            this.portalCooldown = EntityUtil.getPortalCooldown(entity);
        }

        @Override
        public int hashCode() {
            return this.portalBlock.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            Pending p = (Pending) o;
            return BlockUtil.equals(this.portalBlock, p.portalBlock) &&
                   this.entity == p.entity;
        }
    }

    public static interface Callback {
        void onPortalEnter(Block portalBlock, Entity entity, int portalCooldown);
    }
}
