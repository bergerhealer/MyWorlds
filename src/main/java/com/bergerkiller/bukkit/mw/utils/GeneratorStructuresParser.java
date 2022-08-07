package com.bergerkiller.bukkit.mw.utils;

/**
 * Parses whether the no-structures or structures option was set in the generator
 * options string. Removes this option from the string.
 */
public class GeneratorStructuresParser {
    public boolean hasNoStructures = false;
    public boolean hasStructures = false;

    public String process(String options) {
        int idx;
        if (options.startsWith("nostructures;")) {
            options = options.substring(13);
            hasNoStructures = true;
        } else if (options.startsWith("structures;")) {
            options = options.substring(11);
            hasStructures = true;
        } else if ((idx = options.indexOf(";nostructures;")) != -1) {
            options = options.substring(0, idx) + options.substring(idx + 13);
            hasNoStructures = true;
        } else if ((idx = options.indexOf(";structures;")) != -1) {
            options = options.substring(0, idx) + options.substring(idx + 11);
            hasStructures = true;
        } else if (options.endsWith(";nostructures")) {
            options = options.substring(0, options.length() - 13);
            hasNoStructures = true;
        } else if (options.endsWith(";structures")) {
            options = options.substring(0, options.length() - 11);
            hasStructures = true;
        } else if (options.equals("nostructures")) {
            options = "";
            hasNoStructures = true;
        } else if (options.equals("structures")) {
            options = "";
            hasStructures = true;
        }
        return options;
    }
}
