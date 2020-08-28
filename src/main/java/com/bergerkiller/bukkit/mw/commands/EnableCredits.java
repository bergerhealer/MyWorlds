package com.bergerkiller.bukkit.mw.commands;

import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.generated.net.minecraft.server.EntityPlayerHandle;

import net.md_5.bungee.api.ChatColor;

public class EnableCredits extends Command {

    public EnableCredits() {
        super(Permission.COMMAND_ENABLECREDITS, "world.enablecredits");
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
