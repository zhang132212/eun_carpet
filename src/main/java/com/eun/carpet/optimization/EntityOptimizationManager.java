package com.eun.carpet.optimization;

import com.eun.carpet.EUNCarpetSettings;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

public class EntityOptimizationManager {
    private static EntityOptimizationManager instance;
    private final MinecraftServer server;
    private final Map<ServerLevel, HiddenEntityTracker> trackers = new HashMap<>();
    private int scanCooldown = 0;
    private boolean lastOptimizationState = EUNCarpetSettings.entityOptimizationEnabled;

    private EntityOptimizationManager(MinecraftServer server) {
        this.server = server;
    }

    public static void init(MinecraftServer server) {
        instance = new EntityOptimizationManager(server);
    }

    public static EntityOptimizationManager getInstance() {
        return instance;
    }

    public HiddenEntityTracker getTracker(ServerLevel level) {
        return trackers.computeIfAbsent(level, HiddenEntityTracker::new);
    }

    public void tick(MinecraftServer server) {
        if (scanCooldown > 0) {
            scanCooldown--;
        } else {
            scanCooldown = 20;
            performScan();
        }

        for (HiddenEntityTracker tracker : trackers.values()) {
            tracker.tick();
        }

        // 检测规则变化并同步
        boolean currentOptimizationState = EUNCarpetSettings.entityOptimizationEnabled;
        if (lastOptimizationState != currentOptimizationState) {
            lastOptimizationState = currentOptimizationState;
            if (currentOptimizationState) {
                // 规则开启：向所有在线玩家发送当前隐藏实体列表
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    ServerLevel level = player.level();
                    HiddenEntityTracker tracker = getTracker(level);
                    if (tracker != null) {
                        IntSet hiddenIds = tracker.getCurrentHiddenIds();
                        int[] added = hiddenIds.toIntArray();
                        HideEntitiesPayload payload = new HideEntitiesPayload(added, new int[0]);
                        ServerPlayNetworking.send(player, payload);
                    }
                }
            } else {
                // 规则关闭：发送空列表，让客户端取消所有隐藏
                HideEntitiesPayload payload = new HideEntitiesPayload(new int[0], new int[0]);
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(player, payload);
                }
            }
        }
    }

    private void performScan() {
        for (ServerLevel level : server.getAllLevels()) {
            HiddenEntityTracker tracker = trackers.computeIfAbsent(level, HiddenEntityTracker::new);
            tracker.scan();
        }
    }

    public void onLevelUnload(ServerLevel level) {
        trackers.remove(level);
    }

    public void onPlayerChangeDimension(ServerPlayer player, ServerLevel from, ServerLevel to) {
        HiddenEntityTracker tracker = trackers.get(to);
        if (tracker != null) {
            int[] added = tracker.getCurrentHiddenIds().toIntArray();
            HideEntitiesPayload payload = new HideEntitiesPayload(added, new int[0]);
            ServerPlayNetworking.send(player, payload);
        } else {
            HideEntitiesPayload payload = new HideEntitiesPayload(new int[0], new int[0]);
            ServerPlayNetworking.send(player, payload);
        }
    }
}