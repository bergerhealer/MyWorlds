package com.bergerkiller.bukkit.mw.portal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.mw.MyWorlds;

/**
 * Some entities are kept in statis (stop moving) while portal
 * calculations are ongoing.
 */
public class EntityStasisHandler {
    // After this many ticks in stasis, stop trying to freeze the entity
    private static final int EXPIRE_LOGIC_TIME = 200;
    // After this many ticks of nobody putting the entity in stasis, release the entity
    private static final int EXPIRE_TIME = 400;

    private final MyWorlds plugin;
    private final Map<Entity, StasisEntity> _inStasis = new HashMap<>();
    private Task _syncTask;

    public EntityStasisHandler(MyWorlds plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        _syncTask = new Task(plugin) {
            @Override
            public void run() {
                if (!_inStasis.isEmpty()) {
                    int currentTicks = CommonUtil.getServerTicks();
                    Iterator<StasisEntity> iter = _inStasis.values().iterator();
                    while (iter.hasNext()) {
                        if (iter.next().expireOrSync(currentTicks)) {
                            iter.remove();
                        }
                    }
                }
            }
        }.start(1, 1);
    }

    public void disable() {
        Task.stop(_syncTask);
        _syncTask = null;
        _inStasis.clear();
    }

    /**
     * Gets whether a particular entity type can be kept in
     * stasis by this handler while computations are ongoing.
     * 
     * @param entity
     * @return True if this entity can be kept in stasis
     */
    public boolean canBeKeptInStasis(Entity entity) {
        return entity instanceof Item || entity instanceof Projectile || entity instanceof Minecart;
    }

    /**
     * Puts an entity in stasis, freezing the entity position
     * and disabling velocity.
     * 
     * @param entity
     */
    public void putInStasis(Entity entity) {
        _inStasis.computeIfAbsent(entity, StasisEntity::new).store();
    }

    /**
     * Removes an entity from stasis, allowing the entity to move
     * on its own again. Happens automatically when the entity is
     * destroyed, or a stasis timeout expires.
     * 
     * @param entity
     */
    public void freeFromStasis(Entity entity) {
        StasisEntity stasis = _inStasis.remove(entity);
        if (stasis != null) {
            stasis.release();
        }
    }

    private static class StasisEntity {
        public final Entity entity;
        public final Location position;
        public final Vector velocity;
        public final int expireLogicTime;
        public int expireTime;

        public StasisEntity(Entity entity) {
            this.entity = entity;
            this.position = entity.getLocation();
            this.velocity = entity.getVelocity();
            this.expireLogicTime = CommonUtil.getServerTicks() + EXPIRE_LOGIC_TIME;
            entity.setVelocity(new Vector(0.0, 0.04, 0.0));
        }

        // Expires if expired, then returns true, or sync()s and returns false
        public boolean expireOrSync(int currentTicks) {
            if (entity.isDead()) {
                return true;
            } else if (currentTicks > expireTime) {
                release();
                return true;
            } else {
                sync(currentTicks);
                return false;
            }
        }

        public void store() {
            expireTime = CommonUtil.getServerTicks() + EXPIRE_TIME;
        }

        public void sync(int currentTicks) {
            if (currentTicks <= expireLogicTime) {
                entity.teleport(position);
                entity.setVelocity(new Vector(0.0, 0.04, 0.0));
            }
        }

        public void release() {
            entity.teleport(position);
            entity.setVelocity(velocity);
        }
    }
}
