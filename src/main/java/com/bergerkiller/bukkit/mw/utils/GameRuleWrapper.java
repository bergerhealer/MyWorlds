package com.bergerkiller.bukkit.mw.utils;

import com.bergerkiller.bukkit.common.internal.CommonBootstrap;
import org.bukkit.GameRule;
import org.bukkit.World;

import java.util.stream.Stream;

/**
 * A wrapper around a game rule, to allow for different implementations on different server versions.
 * Bukkit keeps changing the game rule constants and there is also the pre-1.13 string-based game rules to support.
 * This class tries to do this in a multi-version compatible way.
 *
 * @param <T> Game Rule value type
 */
public interface GameRuleWrapper<T> {
    GameRuleWrapper<Boolean> NATURAL_HEALTH_REGENERATION = forBoolean("naturalRegeneration", "natural_health_regeneration");
    GameRuleWrapper<Boolean> ADVANCE_TIME = forBoolean("doDaylightCycle", "advance_time");

    /**
     * Gets the value of the game rule for a world
     *
     * @param world World
     * @return Game rule value
     */
    T get(World world);

    /**
     * Sets a new value for the game rule for a world
     *
     * @param world World
     * @param value New game rule value
     */
    void set(World world, T value);

    /**
     * Creates a GameRuleWrapper for a boolean game rule.
     * If on a legacy game version, uses the legacy game rule name. On modern versions, tests for
     * the modern names first, followed by the legacy game rule name.
     *
     * @param legacyGameRuleName Older game rule name that was in use
     * @param modernGameRuleNames Modern game rule names which now use _ in the name instead of camelCase
     * @return GameRuleWrapper
     * @throws UnsupportedOperationException If the game rule is not supported on the server
     */
    static GameRuleWrapper<Boolean> forBoolean(final String legacyGameRuleName, final String... modernGameRuleNames) {
        // Legacy string-based game rules
        if (CommonBootstrap.evaluateMCVersion("<", "1.13.1")) {
            return new GameRuleWrapper<Boolean>() {
                @Override
                @SuppressWarnings("deprecation")
                public Boolean get(World world) {
                    return !"false".equals(world.getGameRuleValue(legacyGameRuleName));
                }

                @Override
                @SuppressWarnings("deprecation")
                public void set(World world, Boolean value) {
                    world.setGameRuleValue(legacyGameRuleName, value ? "true" : "false");
                }
            };
        }

        // Attempt to identify the game rule based on one of the game rule names specified
        //noinspection unchecked
        final GameRule<Boolean> gameRule = (GameRule<Boolean>) Stream.of(GameRule.values())
                .filter(c -> Stream.of(modernGameRuleNames).anyMatch(n -> n.equalsIgnoreCase(c.getName())) || c.getName().equalsIgnoreCase(legacyGameRuleName))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("None of the specified game rules " + String.join(", ", modernGameRuleNames) + ", " + legacyGameRuleName + " exist on this server version"));

        return new GameRuleWrapper<Boolean>() {
            @Override
            public Boolean get(World world) {
                return world.getGameRuleValue(gameRule);
            }

            @Override
            public void set(World world, Boolean value) {
                world.setGameRule(gameRule, value);
            }
        };
    }
}