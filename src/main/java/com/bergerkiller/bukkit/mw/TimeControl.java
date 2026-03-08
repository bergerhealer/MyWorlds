package com.bergerkiller.bukkit.mw;

import com.bergerkiller.bukkit.mw.utils.GameRuleWrapper;
import org.bukkit.World;

public class TimeControl {
    public final WorldConfig config;
    public boolean locking = false;
    private long lockedTime;
    private World world;

    public TimeControl(WorldConfig owner) {
        this.config = owner;
    }

    public void setTime(long time) {
        this.lockedTime = time;
        this.updateWorld(config.getWorld());
        if (world != null) {
            world.setTime(time);
        }
    }

    public long getTime() {
        if (this.world != null) {
            this.lockedTime = world.getTime();
        }
        if (isLocked() || this.world == null) {
            return this.lockedTime;
        } else {
            return this.world.getTime();
        }
    }

    public boolean isLocked() {
        if (this.world != null) {
            this.locking = !GameRuleWrapper.ADVANCE_TIME.get(this.world);
        }
        return this.locking;
    }

    public String getTime(long backup) {
        long time = backup;
        World w = config.getWorld();
        if (w != null) {
            time = w.getTime();
        } else if (this.locking) {
            time = this.lockedTime;
        }
        return Util.formatWorldTime(time);
    }

    /*
     * Sets if the time update task should be running
     * See also: World/Plugin load and unload
     */
    public void setLocking(boolean locking) {
        if (this.locking != locking) {
            this.locking = locking;
            // If world was found then the locking property was automatically set
            if (this.updateWorld(config.getWorld())) {
                return;
            }
            if (this.world != null) {
                GameRuleWrapper.ADVANCE_TIME.set(this.world, !locking);
            }
        }
    }

    public boolean updateWorld(World world) {
        if (this.world != world) {
            this.world = world;
            if (world != null) {
                GameRuleWrapper.ADVANCE_TIME.set(world, !locking);
            }
            return true;
        }
        return false;
    }
}
