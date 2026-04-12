package com.eun.carpet.aioptimization;

import com.eun.carpet.EUNCarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.*;

public class AIOptimizationTracker {
    private final ServerLevel level;
    private final Map<BlockPos, Map<EntityType<?>, AIOptimizedGroup>> groups = new HashMap<>();
    private int scanCooldown = 0;
    private AIOptimizationConfig config;

    public AIOptimizationTracker(ServerLevel level) {
        this.level = level;
        this.config = AIOptimizationConfig.getInstance();
    }

    public void tick() {
        if (!EUNCarpetSettings.aiOptimizationEnabled) {
            groups.values().forEach(map -> map.values().forEach(AIOptimizedGroup::clear));
            groups.clear();
            return;
        }

        if (scanCooldown-- <= 0) {
            scanCooldown = 20;
            scan();
        }

        for (Map<EntityType<?>, AIOptimizedGroup> map : groups.values()) {
            for (AIOptimizedGroup group : map.values()) {
                group.tick(level, config.getCheckInterval(), config.getPositionChangeThreshold());
            }
        }

        groups.values().removeIf(map -> {
            map.values().removeIf(AIOptimizedGroup::isEmpty);
            return map.isEmpty();
        });
    }

    private void scan() {
        config = AIOptimizationConfig.getInstance(); // 每次扫描前刷新配置引用
        Set<EntityType<?>> enabledTypes = config.getEnabledEntityTypes();

        groups.values().forEach(map -> map.values().forEach(AIOptimizedGroup::clear));
        groups.clear();

        Map<BlockPos, Map<EntityType<?>, List<Mob>>> rawGroups = new HashMap<>();
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Mob mob)) continue;
            if (!enabledTypes.contains(mob.getType())) continue;
            BlockPos pos = mob.blockPosition();
            rawGroups.computeIfAbsent(pos, k -> new HashMap<>())
                    .computeIfAbsent(mob.getType(), k -> new ArrayList<>())
                    .add(mob);
        }

        for (Map.Entry<BlockPos, Map<EntityType<?>, List<Mob>>> posEntry : rawGroups.entrySet()) {
            BlockPos pos = posEntry.getKey();
            for (Map.Entry<EntityType<?>, List<Mob>> typeEntry : posEntry.getValue().entrySet()) {
                EntityType<?> type = typeEntry.getKey();
                List<Mob> list = typeEntry.getValue();
                if (list.size() < config.getMinStackSize()) continue;
                boolean allClimbable = list.stream().allMatch(LivingEntity::onClimbable);
                if (!allClimbable) continue;

                AIOptimizedGroup group = new AIOptimizedGroup();
                for (Mob mob : list) {
                    group.addMember(mob);
                }
                group.optimize(level, config.getCheckInterval());
                groups.computeIfAbsent(pos, k -> new HashMap<>()).put(type, group);
            }
        }
    }

    public void clear() {
        groups.values().forEach(map -> map.values().forEach(AIOptimizedGroup::clear));
        groups.clear();
    }
}