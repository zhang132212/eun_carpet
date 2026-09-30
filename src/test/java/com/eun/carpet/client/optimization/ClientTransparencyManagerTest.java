package com.eun.carpet.client.optimization;

import com.eun.carpet.client.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ClientTransparencyManagerTest {
    private static final int[] NONE = new int[0];

    private ClientTransparencyManager manager;
    private MockedStatic<Minecraft> minecraftAccess;
    private MockedStatic<AutoConfig> configAccess;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        Minecraft minecraft = mock(Minecraft.class);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(minecraft).execute(any(Runnable.class));
        minecraftAccess = mockStatic(Minecraft.class);
        minecraftAccess.when(Minecraft::getInstance).thenReturn(minecraft);

        ConfigHolder<ModConfig> configHolder = mock(ConfigHolder.class);
        when(configHolder.getConfig()).thenReturn(new ModConfig());
        configAccess = mockStatic(AutoConfig.class);
        configAccess.when(() -> AutoConfig.getConfigHolder(ModConfig.class)).thenReturn(configHolder);

        manager = ClientTransparencyManager.getInstance();
        manager.clear();
    }

    @AfterEach
    void tearDown() {
        if (manager != null) manager.clear();
        if (configAccess != null) configAccess.close();
        if (minecraftAccess != null) minecraftAccess.close();
    }

    @Test
    void unhiddenEntityDoesNotBecomeHiddenWhenItLoads() {
        manager.update(new int[]{42}, NONE);
        manager.markPending(42);

        manager.update(NONE, new int[]{42});
        assertFalse(manager.shouldHide(42));
        manager.onEntityAdded(entity(42));

        assertFalse(manager.shouldHide(42));
    }

    @Test
    void removalAlsoClearsAnIdWhichIsOnlyPending() {
        manager.markPending(42);

        manager.update(NONE, new int[]{42});
        manager.onEntityAdded(entity(42));

        assertFalse(manager.shouldHide(42));
    }

    @Test
    void removingOnePendingIdPreservesOtherPendingEntities() {
        manager.markPending(42);
        manager.markPending(43);

        manager.update(NONE, new int[]{42});
        manager.onEntityAdded(entity(42));
        manager.onEntityAdded(entity(43));

        assertFalse(manager.shouldHide(42));
        assertTrue(manager.shouldHide(43));
    }

    @Test
    void anExplicitLaterHideStillTakesEffect() {
        manager.update(new int[]{42}, NONE);
        manager.markPending(42);
        manager.update(NONE, new int[]{42});

        manager.update(new int[]{42}, NONE);
        manager.onEntityAdded(entity(42));

        assertTrue(manager.shouldHide(42));
    }

    @Test
    void removalKeepsPrecedenceWhenAnIdAppearsInBothArrays() {
        manager.markPending(42);

        manager.update(new int[]{42}, new int[]{42});
        manager.onEntityAdded(entity(42));

        assertFalse(manager.shouldHide(42));
    }

    private Entity entity(int id) {
        Entity entity = mock(Entity.class);
        when(entity.getId()).thenReturn(id);
        doReturn(EntityTypes.PIG).when(entity).getType();
        return entity;
    }
}
