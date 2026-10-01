package com.eun.carpet.fakeplayer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FakePlayerDataTest {
    @Test
    void savedActionsHaveValueEquality() {
        FakePlayerData.SavedAction first = action("ATTACK", -1, 1, 0, true);
        FakePlayerData.SavedAction second = action("ATTACK", -1, 1, 0, true);

        assertNotSame(first, second);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(List.of(first), List.of(second));
        assertEquals(first, first);
        assertNotEquals(first, null);
        assertNotEquals(first, "ATTACK");
    }

    @Test
    void everySavedActionFieldParticipatesInEquality() {
        FakePlayerData.SavedAction original = action("ATTACK", -1, 1, 0, true);

        assertAll(
                () -> assertNotEquals(original, action("USE", -1, 1, 0, true)),
                () -> assertNotEquals(original, action("ATTACK", 5, 1, 0, true)),
                () -> assertNotEquals(original, action("ATTACK", -1, 2, 0, true)),
                () -> assertNotEquals(original, action("ATTACK", -1, 1, 3, true)),
                () -> assertNotEquals(original, action("ATTACK", -1, 1, 0, false))
        );
    }

    @Test
    void defaultSavedActionsAlsoHaveValueEquality() {
        FakePlayerData.SavedAction first = new FakePlayerData.SavedAction();
        FakePlayerData.SavedAction second = new FakePlayerData.SavedAction();

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, action("ATTACK", 0, 0, 0, false));
    }

    @Test
    void actionComparisonDetectsMovementChanges() {
        FakePlayerData saved = new FakePlayerData();
        FakePlayerData current = new FakePlayerData();
        assertTrue(saved.hasSameActions(current));

        current.setSneaking(true);
        assertFalse(saved.hasSameActions(current));
        saved.updateActionsFrom(current);
        assertTrue(saved.hasSameActions(current));

        current.setSprinting(true);
        assertFalse(saved.hasSameActions(current));
        saved.updateActionsFrom(current);

        current.setForward(1.0F);
        assertFalse(saved.hasSameActions(current));
        saved.updateActionsFrom(current);

        current.setStrafing(-1.0F);
        assertFalse(saved.hasSameActions(current));
        saved.updateActionsFrom(current);
        assertTrue(saved.hasSameActions(current));
    }

    @Test
    void actionComparisonUsesValuesAndDetectsRemovedActions() {
        FakePlayerData saved = new FakePlayerData();
        FakePlayerData current = new FakePlayerData();
        saved.setActions(List.of(action("ATTACK", -1, 1, 0, true)));
        current.setActions(List.of(action("ATTACK", -1, 1, 0, true)));
        assertTrue(saved.hasSameActions(current));

        current.setActions(List.of());
        assertFalse(saved.hasSameActions(current));
        saved.updateActionsFrom(current);
        assertTrue(saved.hasSameActions(current));

        current.setActions(null);
        assertFalse(saved.hasSameActions(current));
    }

    private static FakePlayerData.SavedAction action(String type, int limit, int interval, int offset,
                                                    boolean continuous) {
        return new FakePlayerData.SavedAction(type, limit, interval, offset, continuous);
    }
}
