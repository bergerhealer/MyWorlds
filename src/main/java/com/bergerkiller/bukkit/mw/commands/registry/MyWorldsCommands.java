package com.bergerkiller.bukkit.mw.commands.registry;

import java.util.List;

import org.bukkit.command.BlockCommandSender;
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

    private String trimPluginPrefix(String cmdLabel) {
        return cmdLabel.startsWith("my_worlds:") ? cmdLabel.substring(10) : cmdLabel;
    }

    public void execute(CommandSender sender, String cmdLabel, String[] args) {
        String rootCmdLabel = cmdLabel;
        cmdLabel = trimPluginPrefix(cmdLabel);

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
        Command command = registeredCommand.createExecutor(plugin, sender, rootCmdLabel, cmdLabel, args);
        if (command.hasPermission()) {
            command.execute();
        } else {
            if (command.player != null) {
                command.locmessage(Localization.COMMAND_NOPERM);
            } else if (!(command.sender instanceof BlockCommandSender) && command.allowCommandBlocks()) {
                command.sender.sendMessage("This command is only for players and command blocks!");
            } else {
                command.sender.sendMessage("This command is only for players!");
            }
        }
    }

    public List<String> autocomplete(CommandSender sender, String cmdLabel, String[] args) {
        String rootCmdLabel = cmdLabel;
        cmdLabel = trimPluginPrefix(cmdLabel);

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

        Command command = registeredCommand.createExecutor(plugin, sender, rootCmdLabel, cmdLabel, args);
        return command.autocomplete();
    }
}
