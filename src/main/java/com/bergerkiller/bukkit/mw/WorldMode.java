package com.bergerkiller.bukkit.mw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import com.bergerkiller.bukkit.common.utils.LogicUtil;

public class WorldMode {
    public static final WorldMode NORMAL = new WorldMode(Environment.NORMAL, WorldType.NORMAL, "normal");
    public static final WorldMode NETHER = new WorldMode(Environment.NETHER, WorldType.NORMAL, "nether");
    public static final WorldMode THE_END = new WorldMode(Environment.THE_END, WorldType.NORMAL, "the_end");
    public static final WorldMode FLAT = new WorldMode(Environment.NORMAL, WorldType.FLAT, "flat");
    public static final WorldMode LARGEBIOMES = new WorldMode(Environment.NORMAL, WorldType.LARGE_BIOMES, "largebiomes");
    private static final WorldMode[] values;
    private static final Map<String, WorldMode> byName = new HashMap<String, WorldMode>();
    private final Environment env;
    private final WorldType wtype;
    private final String name;

    static {
        ArrayList<WorldMode> modes = new ArrayList<WorldMode>(100);
        LogicUtil.addArray(modes, NORMAL, NETHER, THE_END, FLAT, LARGEBIOMES);
        for (Environment env : Environment.values()) {
            // Add constants for all worldtypes
            for (WorldType type : WorldType.values()) {
                modes.add(new WorldMode(env, type));
            }
            if (LogicUtil.contains(env, Environment.NORMAL, Environment.NETHER, Environment.THE_END)) {
                continue;
            }
            modes.add(new WorldMode(env, WorldType.NORMAL, env.toString().toLowerCase(Locale.ENGLISH)));
        }
        for (WorldMode mode : modes) {
            byName.put(mode.getName(), mode);
        }
        byName.put("dim1", THE_END);
        byName.put("dim-1", NETHER);
        values = modes.toArray(new WorldMode[0]);
    }

    private WorldMode(Environment env, WorldType wtype) {
        this.env = env;
        this.wtype = wtype;
        StringBuilder nameBuilder = new StringBuilder(20);
        nameBuilder.append(env.toString());
        nameBuilder.append("_");
        if (wtype == WorldType.LARGE_BIOMES) {
            nameBuilder.append("largebiomes");
        } else {
            nameBuilder.append(wtype.toString());
        }
        this.name = nameBuilder.toString().toLowerCase(Locale.ENGLISH);
    }

    private WorldMode(Environment env, WorldType wtype, String name) {
        this.env = env;
        this.wtype = wtype;
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public Environment getEnvironment() {
        return this.env;
    }

    public String getTypeName() {
        if (this.wtype == WorldType.LARGE_BIOMES) {
            return "largeBiomes";
        } else if (this.wtype == WorldType.VERSION_1_1) {
            return "default_1_1";
        } else if (this.wtype == WorldType.NORMAL) {
            return "default";
        }
        return this.wtype.getName().toLowerCase(Locale.ENGLISH);
    }

    public WorldType getType() {
        return this.wtype;
    }

    public void apply(WorldCreator creator) {
        creator.type(this.wtype);
        creator.environment(this.env);
    }

    @Override
    public String toString() {
        return getName() + "{" + env + ", " + wtype + "}";
    }

    public static WorldMode[] values() {
        return values;
    }

    /**
     * Parses a piece of text and obtains the WorldMode stored in it.
     * 
     * @param text to parse
     * @return WorldMode, or NORMAL if none was identified
     */
    public static WorldMode get(String text) {
        return get(text, NORMAL);
    }

    /**
     * Parses a piece of text and obtains the WorldMode stored in it.
     * 
     * @param text to parse
     * @param defaultMode to return upon failure
     * @return WorldMode, or defaultMode if none was identified
     */
    public static WorldMode get(String text, WorldMode defaultMode) {
        String fixedText = text.toLowerCase(Locale.ENGLISH);
        WorldMode mode = byName.get(fixedText);
        int start;
        while (mode == null && (start = fixedText.indexOf('_')) != -1) {
            fixedText = fixedText.substring(start + 1);
            mode = byName.get(fixedText);
        }
        return LogicUtil.fixNull(mode, defaultMode);
    }

    public static WorldMode get(WorldType worldType, Environment environment) {
        for (WorldMode mode : values()) {
            if (mode.getType() == worldType && mode.getEnvironment() == environment) {
                return mode;
            }
        }
        return new WorldMode(environment, worldType);
    }

    public static WorldMode get(World world) {
        return get(world.getWorldType(), world.getEnvironment());
    }
}
