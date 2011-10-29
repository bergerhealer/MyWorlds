package com.bergerkiller.bukkit.mw;

import org.bukkit.World;

public class TimeControl {
	
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
    	if (this.locker == null) {
    		World w = WorldManager.getWorld(worldname);
    		if (w == null) {
    			return getTimeString(backup);
    		} else {
        		return getTimeString(w.getTime());
    		}
    	} else {
    		return getTimeString(this.locker.time);
    	}
    }
            
    public TimeControl(String worldname) {
    	this.worldname = worldname;
    }
    
    public String worldname;
    public Locker locker;

    public void lockTime(long time) {
    	if (this.locker == null) {
    		this.locker = new Locker(worldname, time);
    		this.locker.start();
    	} else {
        	this.locker.time = time;
        	this.locker.run();
    	}
    }
    public void unlockTime() {
    	if (this.locker != null) {
    		this.locker.stop();
    		this.locker = null;
    	}
    }
    public boolean isLocked() {
    	return this.locker != null && this.locker.isRunning();
    }
    
    /*
     * Sets if the time update task should be running
     * See also: World/Plugin load and unload
     */
    public boolean setLocking(boolean locking) {
    	if (this.locker != null) {
    		if (this.locker.isRunning() != locking) {
            	if (locking) { 
            		return this.locker.start();
            	} else {
            		this.locker.stop();
            		return true;
            	}
    		}
    	}
    	return false;
    }
    
    public static class Locker implements Runnable {

    	public Locker(String worldname, long time) {
    		this.worldname = worldname;
    		this.time = time;
    	}
    	
    	private int id = -1;
    	private String worldname;
    	private World w;
    	public long time;
    	
    	public boolean isRunning() {
    		return this.id != -1;
    	}
		@Override
		public void run() {
			WorldManager.setTime(this.w, time);
		}
		
		public void stop() {
			MyWorlds.plugin.getServer().getScheduler().cancelTask(this.id);
			this.id = -1;
		}
		public boolean start() {
			return this.start(MyWorlds.timeLockInterval.get());
		}
		public boolean start(long interval) {
			this.w = WorldManager.getWorld(this.worldname);
			if (this.w == null) return false;
			this.id = MyWorlds.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(MyWorlds.plugin, this, 0, interval);
			return true;
		}

    }
    
}
