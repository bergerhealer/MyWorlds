package com.bergerkiller.bukkit.mw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import com.bergerkiller.bukkit.common.nbt.CommonTag;
import com.bergerkiller.bukkit.common.nbt.CommonTagCompound;
import com.bergerkiller.bukkit.common.nbt.CommonTagList;

/**
 * List of positions a player had on various different worlds. Used by the
 * player data controller of MyWorlds.
 */
public class LastPlayerPositionList implements Cloneable {
    /** World UUID details inside a last-position compound element */
    public static final String DATA_TAG_WORLD = "world";
    /** World name details inside a last-position compound element. Fallback. */
    public static final String DATA_TAG_WORLD_NAME = "worldName";
    /** Position details inside a last-position compound element */
    public static final String DATA_TAG_POS = "pos";
    /** Rotation details inside a last-position compound element */
    public static final String DATA_TAG_ROT = "rot";
    /** Timestamp the player was last on a last-position world */
    public static final String DATA_TAG_TIME = "time";

    private final CommonTagList list;

    public LastPlayerPositionList() {
        this(new CommonTagList());
    }

    public LastPlayerPositionList(CommonTagList list) {
        this.list = list;
    }

    public CommonTagList getDataTag() {
        return this.list;
    }

    public void add(CommonTagCompound worldPosData) {
        this.list.add(worldPosData);
    }

    public void update(LastPosition position, CommonTagCompound worldPosData) {
        if (position == null) {
            this.list.add(worldPosData);
        } else {
            this.list.set(position.index, worldPosData);
        }
    }

    /**
     * Adds a no-position slot. This tells the system that no prior player data
     * exists for this Player on a particular World. Speeds up world rejoin logic
     * to prevent probing a lot of potential player profile files to check for
     * legacy data.<br>
     * <br>
     * Can be removed entirely once legacy player data profiles aren't loaded in anymore.
     *
     * @param worldConfig
     */
    public void addNoPositionSlot(WorldConfig worldConfig) {
        CommonTagCompound tag = new CommonTagCompound();
        {
            World world = worldConfig.getWorld();
            if (world != null) {
                tag.putUUID(DATA_TAG_WORLD, world.getUID());
            }
        }
        tag.putValue(DATA_TAG_WORLD_NAME, worldConfig.worldname);
        tag.putValue(DATA_TAG_TIME, 0L);
        this.list.add(tag);
    }

    /**
     * Removes no-position player data slots of world uuid's that aren't loaded anymore.
     * Prevents an ever-growing list of these in the player data.
     */
    public void cleanupMissingWorldNoPositionSlots() {
        int size = list.size();
        for (int i = size - 1; i >= 0; --i) {
            CommonTag tag = list.get(i);
            if (tag instanceof CommonTagCompound) {
                CommonTagCompound pos = (CommonTagCompound) tag;
                if (!pos.containsKey(DATA_TAG_POS)) {
                    // Remove if world by this UUID is missing (and uuid was used to store)
                    UUID uuid = pos.getUUID(DATA_TAG_WORLD);
                    if (uuid != null) {
                        if (Bukkit.getWorld(uuid) == null) {
                            list.remove(i);
                        }
                        continue;
                    }

                    // Remove if world by this name is missing
                    String name = pos.getValue(DATA_TAG_WORLD_NAME, String.class);
                    if (name == null || Bukkit.getWorld(name) == null) {
                        list.remove(i);
                        continue;
                    }
                }
            }
        }
    }

    public boolean removeForWorld(WorldConfig worldConfig) {
        LastPosition pos = getForWorld(worldConfig);
        if (pos == null) {
            return false;
        }
        list.remove(pos.index);
        return true;
    }

    public List<LastPosition> all(boolean newestFirst) {
        int size = list.size();
        List<LastPosition> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            CommonTag tag = list.get(i);
            if (tag instanceof CommonTagCompound) {
                result.add(new LastPosition((CommonTagCompound) tag, i));
            }
        }

        // Sort list by time
        if (newestFirst) {
            Collections.sort(result, Collections.reverseOrder());
        } else {
            Collections.sort(result);
        }

        return result;
    }

    public boolean containsWorld(WorldConfig worldConfig) {
        return getForWorld(worldConfig) != null;
    }

    public LastPosition getForWorld(WorldConfig worldConfig) {
        // Compare against world UUID if the world is loaded (most common case)
        UUID worldUUID = null;
        {
            World world = worldConfig.getWorld();
            if (world != null) {
                worldUUID = world.getUID();
            }
        }

        int size = list.size();
        for (int i = 0; i < size; i++) {
            CommonTag tag = list.get(i);
            if (tag instanceof CommonTagCompound) {
                CommonTagCompound pos = (CommonTagCompound) tag;

                // Try world UUID
                if (worldUUID != null) {
                    UUID uuid = pos.getUUID(DATA_TAG_WORLD);
                    if (uuid != null) {
                        if (worldUUID.equals(uuid)) {
                            return new LastPosition(pos, i);
                        } else {
                            continue; // UUID mismatch, doesn't match this one
                        }
                    }
                }

                // Try world name
                if (worldConfig.worldname.equals(pos.getValue(DATA_TAG_WORLD_NAME, String.class))) {
                    return new LastPosition(pos, i);
                }
            }
        }

        return null;
    }

    @Override
    public LastPlayerPositionList clone() {
        return new LastPlayerPositionList(this.list.clone());
    }

    @Override
    public String toString() {
        return this.list.toString();
    }

    public static CommonTagCompound createPositionData(Location loc, long timeMillis) {
        CommonTagCompound data = new CommonTagCompound();
        data.putUUID(DATA_TAG_WORLD, loc.getWorld().getUID());
        data.putValue(DATA_TAG_WORLD_NAME, loc.getWorld().getName());
        data.putValue(DATA_TAG_TIME, timeMillis);
        data.putListValues(DATA_TAG_POS, loc.getX(), loc.getY(), loc.getZ());
        data.putListValues(DATA_TAG_ROT, loc.getYaw(), loc.getPitch());
        return data;
    }

    public static class LastPosition implements Comparable<LastPosition> {
        private final CommonTagCompound tag;
        private final int index;

        private LastPosition(CommonTagCompound tag, int index) {
            this.tag = tag;
            this.index = index;
        }

        public UUID getWorldUUID() {
            return tag.getUUID(DATA_TAG_WORLD);
        }

        public String getWorldName() {
            return tag.getValue(DATA_TAG_WORLD_NAME, String.class);
        }

        public World getWorld() {
            UUID uuid = this.getWorldUUID();
            if (uuid != null) {
                return Bukkit.getWorld(uuid);
            }
            String name = this.getWorldName();
            if (name != null) {
                return Bukkit.getWorld(name);
            }
            return null; // Weird!
        }

        public long getTime() {
            return tag.getValue(DATA_TAG_TIME, 0L);
        }

        /**
         * Decodes the position information as a Location. If no information is stored,
         * or the world it is for is not loaded, returns null.
         *
         * @return decoded location
         */
        public Location getLocation() {
            Position p = getPosition();
            return (p == null || p.getWorld() == null) ? null : p;
        }

        /**
         * Decodes the position information. If no information is stored,
         * or the world it is for is completely invalid, returns null.
         *
         * @return decoded position
         */
        public Position getPosition() {
            // Decode position. If missing, return null.
            double x, y, z;
            {
                CommonTag pos = tag.get(DATA_TAG_POS);
                if (!(pos instanceof CommonTagList)) {
                    return null;
                }
                CommonTagList posList = (CommonTagList) pos;
                if (posList.size() != 3) {
                    return null;
                }
                x = posList.getValue(0, 0.0);
                y = posList.getValue(1, 0.0);
                z = posList.getValue(2, 0.0);
            }

            // Include yaw/pitch info if available
            float yaw = 0.0f, pitch = 0.0f;
            {
                CommonTag rot = tag.get(DATA_TAG_ROT);
                if (rot instanceof CommonTagList) {
                    CommonTagList rotList = (CommonTagList) rot;
                    if (rotList.size() == 2) {
                        yaw = rotList.getValue(0, 0.0f);
                        pitch = rotList.getValue(1, 0.0f);
                    }
                }
            }

            // Try loaded world (by UUID)
            UUID uuid = this.getWorldUUID();
            if (uuid != null) {
                World world = Bukkit.getWorld(uuid);
                if (world != null) {
                    return new Position(world, x, y, z, yaw, pitch);
                }
            }

            // Try loaded world (by world name)
            String name = this.getWorldName();
            if (name != null) {
                World world = Bukkit.getWorld(name);
                if (world != null) {
                    if (uuid != null && !uuid.equals(world.getUID())) {
                        return null; // Invalid!
                    }
                    return new Position(world, x, y, z, yaw, pitch);
                } else {
                    // World not loaded. Offline position.
                    return new Position(name, x, y, z, yaw, pitch);
                }
            }

            // No world. Nothing can be done :(
            return null;
        }

        @Override
        public int compareTo(LastPosition o) {
            return Long.compare(this.getTime(), o.getTime());
        }
    }
}
