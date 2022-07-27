package com.bergerkiller.bukkit.mw.commands.registry;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.commands.Command;
import com.bergerkiller.mountiplex.MountiplexUtil;

/**
 * An available /world subcommand, with some details about the command
 * to use in auto-completion.
 */
public class RegisteredCommand {
    private final Supplier<Command> _constructor;
    private final String _name;
    private final String[] _aliases;

    private RegisteredCommand(Supplier<Command> constructor, String name, String[] aliases) {
        _constructor = constructor;
        _name = name;
        _aliases = aliases;
    }

    /**
     * Creates a new instance of this Command for further execution
     * 
     * @param plugin
     * @param sender
     * @param commandRootLabel Root command label ('world', 'myworlds:mw', etc.)
     * @param commandLabel
     * @param args
     * @return command
     */
    public Command createExecutor(MyWorlds plugin, CommandSender sender, String commandRootLabel, String commandLabel, String[] args) {
        Command command = _constructor.get();
        command.init(plugin, sender, commandRootLabel, commandLabel, args);
        return command;
    }

    /**
     * Gets the main command name
     * 
     * @return name
     */
    public String getName() {
        return _name;
    }

    /**
     * Gets some command aliases for this same command, if available
     * 
     * @return command alias names
     */
    public String[] getAliases() {
        return _aliases;
    }

    /**
     * Gets the main command name, and all aliases, as a stream
     * of names
     * 
     * @return stream of names and aliases
     */
    public Stream<String> streamAllNames() {
        if (_aliases.length == 0) {
            return MountiplexUtil.toStream(_name);
        } else {
            return Stream.concat(MountiplexUtil.toStream(_name), Stream.of(_aliases));
        }
    }

    /**
     * Creates a new registered command
     * 
     * @param constructor Constructor for command objects when executed
     * @param name Main name of the command
     * @param aliases Some aliases for the same command
     * @return registered command
     */
    public static RegisteredCommand create(Supplier<Command> constructor, String name, String... aliases) {
        return new RegisteredCommand(constructor, name, aliases);
    }
}
