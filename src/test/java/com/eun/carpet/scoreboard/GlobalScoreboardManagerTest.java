package com.eun.carpet.scoreboard;

import com.eun.carpet.EUNCarpetSettings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.server.ServerScoreboard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class GlobalScoreboardManagerTest {
    private final boolean originalEnabled = EUNCarpetSettings.globalScoreboardEnabled;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @AfterEach
    void restoreRule() {
        EUNCarpetSettings.globalScoreboardEnabled = originalEnabled;
    }

    @Test
    void disabledRotationLeavesUnrelatedSidebarUntouched() {
        ServerScoreboard scoreboard = mock(ServerScoreboard.class);
        Objective external = mock(Objective.class);
        when(external.getName()).thenReturn("server_stats");
        when(scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR)).thenReturn(external);
        initialize(scoreboard);

        for (int i = 0; i < 100; i++) GlobalScoreboardManager.getInstance().tick();

        verify(scoreboard, never()).setDisplayObjective(eq(DisplaySlot.SIDEBAR), any());
    }

    @Test
    void disabledRotationClearsItsOwnSidebarOnce() {
        ServerScoreboard scoreboard = mock(ServerScoreboard.class);
        Objective owned = mock(Objective.class);
        when(owned.getName()).thenReturn("eun_mining");
        when(scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR)).thenReturn(owned).thenReturn(null);
        initialize(scoreboard);

        for (int i = 0; i < 100; i++) GlobalScoreboardManager.getInstance().tick();

        verify(scoreboard).setDisplayObjective(DisplaySlot.SIDEBAR, null);
    }

    private void initialize(ServerScoreboard scoreboard) {
        MinecraftServer server = mock(MinecraftServer.class);
        when(server.getScoreboard()).thenReturn(scoreboard);
        EUNCarpetSettings.globalScoreboardEnabled = false;
        GlobalScoreboardManager.init(server);
    }
}
