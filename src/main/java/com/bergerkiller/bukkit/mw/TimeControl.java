package com.bergerkiller.bukkit.mw;

import org.bukkit.World;

import com.bergerkiller.bukkit.common.Task;

public class TimeControl {
    private static final String LOCKING_RULE = "doDaylightCycle";
    private boolean canUseGameRule;
    public final WorldConfig config;
    public boolean locking = false;
    private long lockedTime;
    private World world;
    private Task lockingTask;

    public TimeControl(WorldConfig owner) {
        this.config = owner;
        this.lockingTask = null;
    }

    public void setTime(long time) {
        this.lockedTime = time;
        this.updateWorld(config.getWorld());
        if (world != null) {
            world.setTime(time);
        }
    }

    public long getTime() {
        if (this.canUseGameRule && this.world != null) {
            this.lockedTime = world.getTime();
        }
        if (isLocked() || this.world == null) {
            return this.lockedTime;
        } else {
            return this.world.getTime();
        }
    }

    public boolean isLocked() {
        if (this.canUseGameRule && this.world != null) {
            this.locking = !this.world.getGameRuleValue(LOCKING_RULE).equalsIgnoreCase("true");
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
                if (canUseGameRule) {
                    // Use the game rule instead of a task
                    this.world.setGameRuleValue(LOCKING_RULE, Boolean.valueOf(!locking).toString());
                } else if (locking) {
                    // Start the locking task
                    this.initLockingTask();
                    this.lockingTask.start();
                } else {
                    // Stop the locking task
                    if (this.lockingTask != null) {
                        this.lockingTask.stop();
                    }
                }
            }
        }
    }

    public boolean updateWorld(World world) {
        if (this.world != world) {
            this.world = world;
            if (world == null && !canUseGameRule) {
                if (this.lockingTask != null) {
                    this.lockingTask.stop();
                }
            } else if (world != null) {
                this.canUseGameRule = this.world.isGameRule(LOCKING_RULE);
                if (canUseGameRule) {
                    this.world.setGameRuleValue(LOCKING_RULE, Boolean.valueOf(!locking).toString());
                } else {
                    this.initLockingTask();
                    this.lockingTask.start();
                }
            }
            return true;
        }
        return false;
    }

    private void initLockingTask() {
        if (this.lockingTask == null) {
            this.lockingTask = new Task(MyWorlds.plugin) {
                @Override
                public Task start() {
                    return this.start(MyWorlds.timeLockInterval, MyWorlds.timeLockInterval);
                }

                @Override
                public void run() {
                    if (locking && world != null) {
                        world.setTime(lockedTime);
                    } else {
                        stop();
                    }
                }
            };
        }
    }
}
