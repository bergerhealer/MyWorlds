package com.bergerkiller.bukkit.mw.commands.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.bergerkiller.bukkit.mw.commands.Command;
import com.bergerkiller.bukkit.mw.commands.UnknownCommand;

/**
 * Base command registry class. Provides registration and lookup
 * fundamentals.
 */
public abstract class CommandRegistry {
    private final List<RegisteredCommand> _commands;
    private final Map<String, RegisteredCommand> _commandsByName;

    public CommandRegistry() {
        _commands = new ArrayList<>();
        _commandsByName = new HashMap<>();
        registerAll();
    }

    /**
     * Searches this registry for the command by this name or alias
     * 
     * @param name
     * @return registered command, null if not found
     */
    public RegisteredCommand find(String name) {
        return _commandsByName.getOrDefault(name.toLowerCase(Locale.ENGLISH), UnknownCommand.REGISTERED);
    }

    /**
     * Produces auto-completion suggestions for a command
     * 
     * @param name
     * @return list of autocompletions, null if there are none
     */
    public List<String> autocomplete(String name) {
        if (name.isEmpty()) {
            // Only show the main commands
            return _commands.stream().map(RegisteredCommand::getName).collect(Collectors.toList());
        } else {
            // First try to match the command exactly
            // If found, only return these results
            final String match = name.toLowerCase(Locale.ENGLISH);
            List<String> matchExact = _commands.stream()
                    .map(RegisteredCommand::getName)
                    .filter(n -> n.startsWith(match))
                    .collect(Collectors.toList());
            if (!matchExact.isEmpty()) {
                return matchExact;
            }

            // Mix in the aliases
            List<String> matchAliases = _commands.stream()
                    .flatMap(RegisteredCommand::streamAllNames)
                    .filter(n -> n.startsWith(match))
                    .collect(Collectors.toList());
            return (matchAliases.isEmpty()) ? null : matchAliases;
        }
    }

    /**
     * Must be implemented to register all the commands for this registry
     */
    protected abstract void registerAll();

    public void register(Supplier<Command> supplier, String name, String... aliases) {
        register(RegisteredCommand.create(supplier, name, aliases));
    }

    public void register(RegisteredCommand command) {
        _commands.add(command);
        _commandsByName.put(command.getName(), command);
        for (String alias : command.getAliases()) {
            // Put the alias, but if another main command is there, keep that one
            RegisteredCommand prev = _commandsByName.put(alias, command);
            if (prev != null && prev.getName().equals(alias)) {
                _commandsByName.put(alias, prev);
            }
        }
    }
}
