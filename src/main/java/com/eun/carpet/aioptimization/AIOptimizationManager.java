package com.eun.carpet.aioptimization;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;

public class AIOptimizationManager {
    private static AIOptimizationManager instance;
    private final Map<ServerLevel, AIOptimizationTracker> trackers = new HashMap<>();
    private MinecraftServer server;

    public static void init(MinecraftServer server) {
        instance = new AIOptimizationManager();
        instance.server = server;
    }

    public static AIOptimizationManager getInstance() { return instance; }

    public void tick() {
        if (server == null) return;
        for (ServerLevel level : server.getAllLevels()) {
            AIOptimizationTracker tracker = trackers.computeIfAbsent(level, AIOptimizationTracker::new);
            tracker.tick();
        }
    }

    public void reloadConfig() {
        // 清空tracker缓存，让下次扫描时重新读取配置
        trackers.clear();
    }
}