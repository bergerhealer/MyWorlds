package com.bergerkiller.bukkit.mw.portal;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.component.LibraryComponent;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.mw.MyWorlds;
import org.bukkit.entity.Entity;

import java.util.HashSet;
import java.util.Set;

/**
 * De-bounces the handling of teleportation to the same portal destination configuration
 * in the same tick.
 */
public class PortalDestinationDebouncer {
    private final Task cleanupTask;
    private final Set<Entry> activeEntries = new HashSet<>();

    public PortalDestinationDebouncer(MyWorlds plugin) {
        this.cleanupTask = new Task(plugin) {
            @Override
            public void run() {
                activeEntries.clear();
            }
        };
    }

    public boolean tryDestination(Entity entity, PortalDestination destination) {
        if (activeEntries.add(new Entry(entity, destination))) {
            if (activeEntries.size() == 1) {
                cleanupTask.start(1);
            }
            return true;
        } else {
            return false;
        }
    }

    private static class Entry {
        public final Entity entity;
        public final PortalDestination destination;
        public final int currentTick;

        public Entry(Entity entity, PortalDestination destination) {
            this.entity = entity;
            this.destination = destination;
            this.currentTick = CommonUtil.getServerTicks();
        }

        @Override
        public int hashCode() {
            return entity.hashCode() ^ destination.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            Entry other = (Entry) o;
            return entity == other.entity &&
                    currentTick == other.currentTick &&
                    destination.equals(other.destination);
        }
    }
}
