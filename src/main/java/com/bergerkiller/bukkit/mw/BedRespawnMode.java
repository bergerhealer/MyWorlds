package com.bergerkiller.bukkit.mw;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * What to do with the bed respawn point (or nether anchor) for a World
 */
public enum BedRespawnMode {
    /** Disables players being able to set their bed as a respawn point, and won't use it when players die */
    DISABLED(false, false),
    /** Enables players to save their bed respawn point, and uses it when players die in the world */
    ENABLED(true, true),
    /** Only allows saving your bed respawn point. Does not use the bed when players die */
    SAVE_ONLY(false, true);

    private final boolean useWhenRespawning;
    private final boolean persistInProfile;

    BedRespawnMode(boolean useWhenRespawning, boolean persistInProfile) {
        this.useWhenRespawning = useWhenRespawning;
        this.persistInProfile = persistInProfile;
    }

    /**
     * Whether the bed respawn point saved in the player's profile should be used when the player
     * respawns on the world due to death.
     *
     * @return True if the bed respawn point should be used when respawning
     */
    public boolean useWhenRespawning() {
        return useWhenRespawning;
    }

    /**
     * Whether the bed respawn point in the player's profile persists on this world. If not, then
     * the information is cleared when loading the player profile, and cannot be saved on this world
     * when sleeping in a bed.
     *
     * @return True if the bed respawn point is persisted in the player profile
     */
    public boolean persistInProfile() {
        return persistInProfile;
    }

    public void showAsMessage(CommandSender sender, String worldName) {
        switch (this) {
            case ENABLED:
                sender.sendMessage(ChatColor.YELLOW + "Respawning at last slept beds on World: '" + ChatColor.WHITE + worldName +
                        ChatColor.YELLOW + "' is " + ChatColor.GREEN + "ENABLED");
                sender.sendMessage(ChatColor.GREEN + "Players will respawn at the bed they last slept in, if set");
                break;
            case DISABLED:
                sender.sendMessage(ChatColor.YELLOW + "Respawning at last slept beds on World: '" + ChatColor.WHITE + worldName +
                        ChatColor.YELLOW + "' is " + ChatColor.RED + "DISABLED");
                sender.sendMessage(ChatColor.YELLOW + "Players will respawn at the world's spawn or home point when dying");
                break;
            case SAVE_ONLY:
                sender.sendMessage(ChatColor.YELLOW + "Respawning at last slept beds on World: '" + ChatColor.WHITE + worldName +
                        ChatColor.YELLOW + "' is " + ChatColor.GREEN + "ENABLED (saving only)");
                sender.sendMessage(ChatColor.YELLOW + "Players will respawn at the world's spawn or home point when dying");
                sender.sendMessage(ChatColor.YELLOW + "However, they can set their bed in this world, and it will be used by (if set) respawn portals");
                break;
        }
    }

    public static BedRespawnMode parse(String text) {
        if (ParseUtil.isBool(text)) {
            return ParseUtil.parseBool(text)
                    ? BedRespawnMode.ENABLED : BedRespawnMode.DISABLED;
        } else {
            return ParseUtil.parseEnum(text, BedRespawnMode.ENABLED);
        }
    }
}
