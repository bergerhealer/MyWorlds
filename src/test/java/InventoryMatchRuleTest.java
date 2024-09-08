import com.bergerkiller.bukkit.mw.WorldInventory;
import org.junit.Test;

import java.util.Iterator;
import java.util.TreeSet;

import static org.junit.Assert.*;

public class InventoryMatchRuleTest {

    @Test
    public void testAnyMatchRuleSortedBack() {
        TreeSet<WorldInventory.MatchRule> rules = new TreeSet<>();
        rules.add(WorldInventory.MatchRule.of("*"));
        rules.add(WorldInventory.MatchRule.of("*"));
        rules.add(WorldInventory.MatchRule.of("^.*$"));
        rules.add(WorldInventory.MatchRule.of("world*"));
        assertEquals(2, rules.size());
        Iterator<WorldInventory.MatchRule> iter = rules.iterator();
        assertEquals("world*", iter.next().getExpression());
        assertEquals("*", iter.next().getExpression());
    }

    @Test
    public void testMatchRuleAny() {
        WorldInventory.MatchRule rule = WorldInventory.MatchRule.of("*");
        assertTrue(rule.matches("world"));
        assertTrue(rule.matches("world2"));
        assertTrue(rule.matches("world_5_nether"));
    }

    @Test
    public void testMatchRuleRegex() {
        WorldInventory.MatchRule rule = WorldInventory.MatchRule.of("^world_.+_[0-9]+$");
        assertTrue(rule.matches("world_cool_2"));
        assertTrue(rule.matches("world_cool_22"));
        assertFalse(rule.matches("world_cool"));
        assertFalse(rule.matches("world_cool_"));
        assertFalse(rule.matches("world_cool_a"));
        assertFalse(rule.matches("world__2"));
        assertFalse(rule.matches("aworld_cool_2"));
    }

    @Test
    public void testMatchRuleAnySimple() {
        WorldInventory.MatchRule rule = WorldInventory.MatchRule.of("world_*");
        assertTrue(rule.matches("world_2"));
        assertTrue(rule.matches("world_22"));
        assertTrue(rule.matches("world_cool"));
        assertFalse(rule.matches("world"));
        assertFalse(rule.matches("aworld_2"));
    }

    @Test
    public void testMatchRuleNumberSimple() {
        WorldInventory.MatchRule rule = WorldInventory.MatchRule.of("world_#");
        assertTrue(rule.matches("world_2"));
        assertTrue(rule.matches("world_22"));
        assertFalse(rule.matches("world_cool"));
        assertFalse(rule.matches("world"));
        assertFalse(rule.matches("aworld_2"));
    }
}
