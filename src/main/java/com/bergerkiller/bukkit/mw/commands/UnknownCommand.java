package com.bergerkiller.bukkit.mw.commands;

import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.commands.registry.RegisteredCommand;

/**
 * Default fallback help command when the command is unknown
 */
public class UnknownCommand extends Command {
    public static RegisteredCommand REGISTERED = RegisteredCommand.create(UnknownCommand::new, "help");

    public UnknownCommand() {
        super(null, null);
    }

    @Override
    public void execute() {
        //This is executed when no command was found
        Localization.COMMAND_UNKNOWN.message(sender, commandLabel);
        Localization.COMMAND_HELP.message(sender);
    }
}
