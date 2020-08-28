package com.bergerkiller.bukkit.mw.commands.registry;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.commands.Command;
import com.bergerkiller.bukkit.mw.commands.TeleportPortal;
import com.bergerkiller.bukkit.mw.commands.UnknownCommand;

public class MyWorldsCommands {
    private final WorldCommandRegistry CMD_WORLD_REGISTRY = new WorldCommandRegistry();
    private final MyWorlds plugin;

    public MyWorldsCommands(MyWorlds plugin) {
        this.plugin = plugin;
    }

    public void execute(CommandSender sender, String cmdLabel, String[] args) {
        //generate a node from this command
        RegisteredCommand registeredCommand = null;
        if (CMD_WORLD_REGISTRY.isWorldCommand(cmdLabel) && args.length >= 1) {
            cmdLabel = args[0];
            args = StringUtil.remove(args, 0);
            registeredCommand = CMD_WORLD_REGISTRY.find(cmdLabel);
        } else if (cmdLabel.equalsIgnoreCase("tpp")) {
            registeredCommand = TeleportPortal.REGISTERED;
        } else {
            registeredCommand = UnknownCommand.REGISTERED;
        }

        // Instantiate
        Command command = registeredCommand.createExecutor(plugin, sender, cmdLabel, args);
        if (command.hasPermission()) {
            command.execute();
        } else {
            if (command.player == null) {
                command.sender.sendMessage("This command is only for players!");
            } else {
                command.locmessage(Localization.COMMAND_NOPERM);
            }
        }
    }

    public List<String> autocomplete(CommandSender sender, String cmdLabel, String[] args) {
        RegisteredCommand registeredCommand = null;
        if (CMD_WORLD_REGISTRY.isWorldCommand(cmdLabel)) {
            if (args.length <= 1) {
                String commandName = (args.length == 0) ? "" : args[0];
                return CMD_WORLD_REGISTRY.autocomplete(commandName);
            }
            if (args.length > 1) {
                // Ask the command after matching
                cmdLabel = args[0];
                args = StringUtil.remove(args, 0);
                registeredCommand = CMD_WORLD_REGISTRY.find(cmdLabel);
            } else {
                String commandName = (args.length == 0) ? "" : args[0];
                return CMD_WORLD_REGISTRY.autocomplete(commandName);
            }
        } else if (cmdLabel.equalsIgnoreCase("tpp")) {
            registeredCommand = TeleportPortal.REGISTERED;
        } else {
            return null; // dunno
        }

        Command command = registeredCommand.createExecutor(plugin, sender, cmdLabel, args);
        return command.autocomplete();
    }
}
