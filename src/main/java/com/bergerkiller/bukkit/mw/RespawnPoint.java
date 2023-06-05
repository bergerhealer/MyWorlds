package com.bergerkiller.bukkit.mw;

import java.util.function.Function;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;

/**
 * Represents the respawn point configuration of a World
 */
public abstract class RespawnPoint {
    public static final RespawnPoint DEFAULT = new RespawnPointCurrentWorldSpawn();
    public static final RespawnPoint IGNORED = new RespawnPointIgnored();
    private final RespawnPoint.Type type;

    private RespawnPoint(RespawnPoint.Type type) {
        this.type = type;
    }

    /**
     * Gets the exact coordinates where the player should respawn after dying in the World
     *
     * @param player Player that is respawning
     * @param world World where the player died and wants to respawn from
     * @return respawn location. <i>null</i> if the plugin shouldn't change the respawn location
     */
    public abstract Location get(Player player, World world);

    protected abstract void writeToConfig(ConfigurationNode config);

    public abstract String getDescription();

    /**
     * Generates configuration that uniquely de-serializes as this respawn point
     *
     * @return respawn point config
     */
    public ConfigurationNode toConfig() {
        ConfigurationNode config = new ConfigurationNode();
        config.set("type", this.type);
        writeToConfig(config);
        return config;
    }

    /**
     * Adjusts this respawn point so that, if this respawn point used to refer to
     * the old world name, it is changed to be the new world name. This is done after
     * a world is copied.
     *
     * @param oldWorldName Old world name to match
     * @param newWorldName New world name to set if it matches
     * @return this, or a new updated respawn point
     */
    public RespawnPoint adjustAfterCopy(String oldWorldName, String newWorldName) {
        return this;
    }

    /**
     * Selects and loads the respawn point from a configuration
     *
     * @param config
     * @return respawn point
     */
    public static RespawnPoint fromConfig(ConfigurationNode config) {
        RespawnPoint.Type type = config.get("type", RespawnPoint.Type.class);
        if (type != null) {
            try {
                return type.create(config);
            } catch (Throwable t) {
                MyWorlds.plugin.getLogger().log(Level.SEVERE, "Broken respawn point config: " + config.toString());
                MyWorlds.plugin.getLogger().log(Level.SEVERE, "Failed to load respawn point config", t);
            }
        }

        // Fallback
        return DEFAULT;
    }

    /**
     * Special respawn point type which does nothing at all, letting Vanilla
     * or another plugin handle the respawning behavior for that world.
     */
    private static class RespawnPointIgnored extends RespawnPoint {
        public RespawnPointIgnored() {
            super(RespawnPoint.Type.IGNORED);
        }

        @Override
        protected void writeToConfig(ConfigurationNode config) {
        }

        @Override
        public Location get(Player player, World world) {
            return null;
        }

        @Override
        public String getDescription() {
            return "be ignored: handled by the server and other plugins";
        }
    }

    /**
     * Respawn at the current spawn point set where the player died.
     * Same as RespawnPointWorldSpawn in behavior. used as fallback if no
     * respawn point could be calculated.
     */
    private static class RespawnPointCurrentWorldSpawn extends RespawnPoint {
        public RespawnPointCurrentWorldSpawn() {
            super(RespawnPoint.Type.WORLD_SPAWN); // Eh. Not really used.
        }

        @Override
        protected void writeToConfig(ConfigurationNode config) {
        }

        @Override
        public Location get(Player player, World world) {
            return WorldConfig.get(world).getSpawnLocation();
        }

        @Override
        public String getDescription() {
            return "the current World spawn point";
        }
    }

    /**
     * Respawn at a location somewhere in a world.
     */
    public static class RespawnPointLocation extends RespawnPoint {
        private final Position position;

        public RespawnPointLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
            this(new Position(worldName, x, y, z, yaw, pitch));
        }

        public RespawnPointLocation(Location location) {
            this(new Position(location));
        }

        private RespawnPointLocation(Position position) {
            super(RespawnPoint.Type.LOCATION);
            this.position = position;
        }

        private RespawnPointLocation(ConfigurationNode config) {
            super(RespawnPoint.Type.LOCATION);

            // Read all required fields, verify they exist
            String worldName = readAndCheckMissing(config, "world", String.class);
            double x = readAndCheckMissing(config, "x", Double.class);
            double y = readAndCheckMissing(config, "y", Double.class);
            double z = readAndCheckMissing(config, "z", Double.class);
            float yaw = config.get("yaw", 0.0f);
            float pitch = config.get("pitch", 0.0f);
            this.position = new Position(worldName, x, y, z, yaw, pitch);
        }

        @Override
        protected void writeToConfig(ConfigurationNode config) {
            config.set("world", this.position.getWorldName());
            config.set("x", this.position.getX());
            config.set("y", this.position.getY());
            config.set("z", this.position.getZ());
            config.set("yaw", this.position.getYaw());
            config.set("pitch", this.position.getPitch());
        }

        @Override
        public RespawnPoint adjustAfterCopy(String oldWorldName, String newWorldName) {
            if (oldWorldName.equals(this.position.getWorldName())) {
                Position updated = this.position.clone();
                updated.setWorldName(newWorldName);
                return new RespawnPointLocation(updated);
            }
            return this;
        }

        @Override
        public Location get(Player player, World world) {
            World atWorld = this.position.getWorld();
            if (atWorld != null) {
                return this.position.toLocation(atWorld);
            }

            // Fallback
            Localization.WORLD_NOTLOADED.message(player, this.position.getWorldName());
            return DEFAULT.get(player, world);
        }

        @Override
        public String getDescription() {
            return "the position x=" + this.position.getX() + " y=" + this.position.getY() + " z=" + this.position.getZ() +
                    " in world '" + this.position.getWorldName() + "'";
        }
    }

    /**
     * Respawn at the spawn point set for a World
     */
    public static class RespawnPointWorldSpawn extends RespawnPoint {
        private final String worldName;

        public RespawnPointWorldSpawn(World world) {
            super(RespawnPoint.Type.WORLD_SPAWN);
            this.worldName = world.getName();
        }

        public RespawnPointWorldSpawn(String worldName) {
            super(RespawnPoint.Type.WORLD_SPAWN);
            this.worldName = worldName;
        }

        private RespawnPointWorldSpawn(ConfigurationNode config) {
            super(RespawnPoint.Type.WORLD_SPAWN);
            this.worldName = readAndCheckMissing(config, "world", String.class);
        }

        @Override
        protected void writeToConfig(ConfigurationNode config) {
            config.set("world", this.worldName);
        }

        @Override
        public RespawnPoint adjustAfterCopy(String oldWorldName, String newWorldName) {
            if (oldWorldName.equals(this.worldName)) {
                return new RespawnPointWorldSpawn(newWorldName);
            }
            return this;
        }

        @Override
        public Location get(Player player, World world) {
            WorldConfig config = WorldConfig.getIfExists(this.worldName);
            Location loc;
            if (config != null && (loc = config.getSpawnLocation()) != null) {
                return loc;
            }

            // Fallback
            Localization.WORLD_NOTLOADED.message(player, this.worldName);
            return DEFAULT.get(player, world);
        }

        @Override
        public String getDescription() {
            return "the spawn point of World '" + this.worldName + "'";
        }
    }

    /**
     * Respawn at a portal
     */
    public static class RespawnPointPortal extends RespawnPoint {
        private final String portalName;

        public RespawnPointPortal(String portalName) {
            super(RespawnPoint.Type.PORTAL);
            this.portalName = portalName;
        }

        private RespawnPointPortal(ConfigurationNode config) {
            super(RespawnPoint.Type.PORTAL);
            this.portalName = readAndCheckMissing(config, "portal", String.class);
        }

        @Override
        protected void writeToConfig(ConfigurationNode config) {
            config.set("portal", this.portalName);
        }

        @Override
        public Location get(Player player, World world) {
            Location loc = PortalStore.getPortalLocation(this.portalName, world.getName(), true, player);
            if (loc != null) {
                return loc;
            }

            // Fallback
            Localization.PORTAL_NOTFOUND.message(player, this.portalName);
            return DEFAULT.get(player, world);
        }

        @Override
        public String getDescription() {
            return "the portal '" + this.portalName + "'";
        }
    }

    /**
     * Respawns the Player where the player died
     */
    public static class RespawnPointPreviousLocation extends RespawnPoint {

        public RespawnPointPreviousLocation() {
            super(RespawnPoint.Type.PREVIOUS);
        }

        public RespawnPointPreviousLocation(ConfigurationNode config) {
            super(RespawnPoint.Type.PREVIOUS);
        }

        @Override
        public Location get(Player player, World world) {
            return player.getLocation();
        }

        @Override
        protected void writeToConfig(ConfigurationNode config) {
        }

        @Override
        public String getDescription() {
            return "the position where the player died";
        }
    }

    private static <T> T readAndCheckMissing(ConfigurationNode config, String name, Class<T> type) {
        T value = config.get(name, type);
        if (value == null) {
            throw new IllegalArgumentException("Required entry missing in configuration: " + name);
        }
        return value;
    }

    public static enum Type {
        LOCATION(RespawnPointLocation::new),
        WORLD_SPAWN(RespawnPointWorldSpawn::new),
        PORTAL(RespawnPointPortal::new),
        PREVIOUS(RespawnPointPreviousLocation::new),
        IGNORED(cfg -> RespawnPoint.IGNORED);

        private final Function<ConfigurationNode, RespawnPoint> factory;

        private Type(Function<ConfigurationNode, RespawnPoint> factory) {
            this.factory = factory;
        }

        public RespawnPoint create(ConfigurationNode config) {
            return factory.apply(config);
        }
    }
}
