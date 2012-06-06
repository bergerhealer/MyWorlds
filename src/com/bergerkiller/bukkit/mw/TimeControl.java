package com.bergerkiller.bukkit.mw;

import org.bukkit.World;

import com.bergerkiller.bukkit.common.Task;

public class TimeControl extends Task {
	
	/**
	 * Returns the time value based on a name
	 * Returns -1 if no time format was detected
	 * Some credits go to CommandBook for their name<>time table!
	 * @param timeName
	 */
	public static long getTime(String timeName) {
		try {
			String[] bits = timeName.split(":");
			if (bits.length == 2) {
				long hours = 1000 * (Long.parseLong(bits[0]) - 8);
				long minutes = 1000 * Long.parseLong(bits[1]) / 60;
				return hours + minutes;
			} else {
				return (long) ((Double.parseDouble(timeName) - 8) * 1000) ;
			}
		} catch (Exception ex) {
	        // Or some shortcuts
	        if (timeName.equalsIgnoreCase("dawn")) {
	            return 22000;
	        } else if (timeName.equalsIgnoreCase("sunrise")) {
	            return 23000;
	        } else if (timeName.equalsIgnoreCase("morning")) {
	            return 24000;
	        } else if (timeName.equalsIgnoreCase("day")) {
	            return 24000;
	        } else if (timeName.equalsIgnoreCase("midday")) {
	        	return 28000;
	        } else if (timeName.equalsIgnoreCase("noon")) {
	            return 28000;
	        } else if (timeName.equalsIgnoreCase("afternoon")) {
	            return 30000;
	        } else if (timeName.equalsIgnoreCase("evening")) {
	            return 32000;
	        } else if (timeName.equalsIgnoreCase("sunset")) {
	            return 37000;
	        } else if (timeName.equalsIgnoreCase("dusk")) {
	            return 37500;
	        } else if (timeName.equalsIgnoreCase("night")) {
	            return 38000;
	        } else if (timeName.equalsIgnoreCase("midnight")) {
	            return 16000;
	        }
		}
		return -1;
	}
	
	/**
	 * CommandBook getTime function, credit go to them for this!
	 * @param time - The time to parse
	 * @return The name of this time
	 */
    public static String getTimeString(long time) {
        int hours = (int) ((time / 1000 + 8) % 24);
        int minutes = (int) (60 * (time % 1000) / 1000);
        return String.format("%02d:%02d (%d:%02d %s)",
                hours, minutes, (hours % 12) == 0 ? 12 : hours % 12, minutes,
                hours < 12 ? "am" : "pm");
    }

    public String getTime(long backup) {
    	if (!this.locking) {
    		World w = config.getWorld();
    		if (w == null) {
    			return getTimeString(backup);
    		} else {
        		return getTimeString(w.getTime());
    		}
    	} else {
    		return getTimeString(this.lockedTime);
    	}
    }

    public TimeControl(WorldConfig owner) {
    	super(MyWorlds.plugin);
    	this.config = owner;
    }

    public final WorldConfig config;
    public boolean locking = false;
    private long lockedTime;
    private long realtime;
    private World world;

    public void setTime(long time) {
    	this.lockedTime = this.realtime = time;
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
		WorldManager.setTime(this.world, realtime);
		this.realtime += 24000L;
	}

	@Override
	public Task start() {
		this.start(MyWorlds.timeLockInterval, MyWorlds.timeLockInterval);
		return this;
	}
}
