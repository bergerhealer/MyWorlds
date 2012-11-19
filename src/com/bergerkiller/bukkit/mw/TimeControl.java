package com.bergerkiller.bukkit.mw;

import org.bukkit.World;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.TimeUtil;

public class TimeControl extends Task {
    public String getTime(long backup) {
    	if (!this.locking) {
    		World w = config.getWorld();
    		if (w == null) {
    			return TimeUtil.getTimeString(backup);
    		} else {
        		return TimeUtil.getTimeString(w.getTime());
    		}
    	} else {
    		return TimeUtil.getTimeString(this.lockedTime);
    	}
    }

    public TimeControl(WorldConfig owner) {
    	super(MyWorlds.plugin);
    	this.config = owner;
    }

    public final WorldConfig config;
    public boolean locking = false;
    private long lockedTime;
    private World world;

    public void setTime(long time) {
    	this.lockedTime = time;
    	this.updateWorld(config.getWorld());
    	if (world != null) {
    		this.run();
    	}
    }
    public long getTime() {
    	return lockedTime;
    }
    public boolean isLocked() {
    	return this.locking;
    }
    /*
     * Sets if the time update task should be running
     * See also: World/Plugin load and unload
     */
    public void setLocking(boolean locking) {
		if (this.locking != locking) {
			this.locking = locking;
        	if (locking) {
        		this.updateWorld(config.getWorld());
        	} else {
        		this.stop();
        	}
		}
    }
    public void updateWorld(World world) {
		this.world = world;
    	if (this.locking) {
    		if (this.world == null) {
    			if (this.isRunning()) {
    				this.stop();
    			}
    		} else {
    			if (!this.isRunning()) {
    				this.start();
    			}
    		}
    	}
    }

	@Override
	public void run() {
		if (this.world == null) {
			this.stop();
		} else {
			this.world.setTime(this.lockedTime);
		}
	}

	@Override
	public Task start() {
		return this.start(MyWorlds.timeLockInterval, MyWorlds.timeLockInterval);
	}
}
