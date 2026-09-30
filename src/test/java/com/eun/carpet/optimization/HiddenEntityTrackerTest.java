package com.eun.carpet.optimization;

import com.eun.carpet.EUNCarpetSettings;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class HiddenEntityTrackerTest {
    @TempDir
    static Path configDirectory;

    private ServerLevel level;
    private EntityType<?> type;
    private HiddenEntityTracker tracker;
    private EntityOptimizationConfig config;
    private MockedStatic<EntityOptimizationConfig> configAccess;
    private boolean previousOptimizationEnabled;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void setUp() {
        previousOptimizationEnabled = EUNCarpetSettings.entityOptimizationEnabled;
        EUNCarpetSettings.entityOptimizationEnabled = true;

        // The config class resolves its directory during static initialization.
        FabricLoader loader = mock(FabricLoader.class);
        when(loader.getConfigDir()).thenReturn(configDirectory);
        try (MockedStatic<FabricLoader> loaderAccess = mockStatic(FabricLoader.class)) {
            loaderAccess.when(FabricLoader::getInstance).thenReturn(loader);
            config = new EntityOptimizationConfig();
            configAccess = mockStatic(EntityOptimizationConfig.class);
        }
        config.minStackSize = 2;
        config.smoothingAlpha = 0.5;
        configAccess.when(EntityOptimizationConfig::getInstance).thenReturn(config);

        level = mock(ServerLevel.class);
        type = mock(EntityType.class);
        tracker = new HiddenEntityTracker(level);
    }

    @AfterEach
    void tearDown() {
        if (configAccess != null) configAccess.close();
        EUNCarpetSettings.entityOptimizationEnabled = previousOptimizationEnabled;
    }

    @Test
    void removesSmoothingHistoryWhenGroupDisappears() throws ReflectiveOperationException {
        scanGroup(new BlockPos(0, 64, 0), 4);
        assertEquals(1, areaStates().size());

        when(level.getAllEntities()).thenReturn(List.of());
        tracker.scan();

        assertEquals(0, areaStates().size());
    }

    @Test
    void removesHistoryAtAndBelowTheMinimumStackSize() throws ReflectiveOperationException {
        BlockPos position = new BlockPos(0, 64, 0);
        scanGroup(position, 4);
        scanGroup(position, config.minStackSize);
        assertEquals(0, areaStates().size());

        scanGroup(position, 4);
        scanGroup(position, config.minStackSize - 1);
        assertEquals(0, areaStates().size());
    }

    @Test
    void preservesSmoothingForContinuouslyActiveGroups() throws ReflectiveOperationException {
        BlockPos position = new BlockPos(0, 64, 0);
        scanGroup(position, 4);
        Object originalState = onlyAreaState();

        scanGroup(position, 8);

        assertSame(originalState, onlyAreaState());
        assertEquals(6.0, smoothCount(onlyAreaState()));
    }

    @Test
    void returningGroupStartsWithItsCurrentCount() throws ReflectiveOperationException {
        BlockPos position = new BlockPos(0, 64, 0);
        scanGroup(position, 4);
        Object originalState = onlyAreaState();
        scanGroup(position, config.minStackSize);

        scanGroup(position, 8);

        assertNotSame(originalState, onlyAreaState());
        assertEquals(8.0, smoothCount(onlyAreaState()));
    }

    @Test
    void historyDoesNotAccumulateAsAStackMoves() throws ReflectiveOperationException {
        for (int x = 0; x < 32; x++) {
            scanGroup(new BlockPos(x, 64, 0), 4);
            assertEquals(1, areaStates().size(), "Only the current active block should retain history");
        }
    }

    private void scanGroup(BlockPos position, int count) {
        List<Entity> entities = new ArrayList<>();
        for (int id = 0; id < count; id++) {
            Entity entity = mock(Entity.class);
            when(entity.getId()).thenReturn(id);
            when(entity.blockPosition()).thenReturn(position);
            doReturn(type).when(entity).getType();
            entities.add(entity);
        }
        when(level.getAllEntities()).thenReturn(entities);
        tracker.scan();
    }

    private Map<?, ?> areaStates() throws ReflectiveOperationException {
        Field field = HiddenEntityTracker.class.getDeclaredField("areaStates");
        field.setAccessible(true);
        return (Map<?, ?>) field.get(tracker);
    }

    private Object onlyAreaState() throws ReflectiveOperationException {
        assertEquals(1, areaStates().size());
        return areaStates().values().iterator().next();
    }

    private static double smoothCount(Object state) throws ReflectiveOperationException {
        Field field = state.getClass().getDeclaredField("smoothCount");
        field.setAccessible(true);
        return field.getDouble(state);
    }
}
