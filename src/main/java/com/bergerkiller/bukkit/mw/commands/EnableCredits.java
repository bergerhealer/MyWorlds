package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.generated.net.minecraft.server.level.EntityPlayerHandle;

public class EnableCredits extends Command {

    public EnableCredits() {
        super(Permission.COMMAND_ENABLECREDITS, "world.showcredits");
    }

    @Override
    public boolean allowConsole() {
        return false;
    }

    public void execute() {
        EntityPlayerHandle.fromBukkit(player).setHasSeenCredits(false);
        player.sendMessage(ChatColor.GREEN + "End game credits will be shown again next time you finish the end");
    }
}
