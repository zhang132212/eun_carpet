package com.eun.carpet.listener;

import com.eun.carpet.commandgui.ServerCommandConfigManager;
import com.eun.carpet.fakeplayer.FakePlayerManager;
import com.eun.carpet.highlight.HighlightManager;
import com.eun.carpet.optimization.HideEntitiesPayload;
import com.eun.carpet.optimization.EntityOptimizationManager;
import com.eun.carpet.optimization.HiddenEntityTracker;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerEventListener {
    private static PlayerEventListener instance;
    private final MinecraftServer server;
    private final Map<UUID, ResourceKey<Level>> playerDimensions = new ConcurrentHashMap<>();

    private PlayerEventListener(MinecraftServer server) {
        this.server = server;
    }

    public static void init(MinecraftServer server) {
        instance = new PlayerEventListener(server);
    }

    public static PlayerEventListener getInstance() {
        return instance;
    }

    public void onPlayerLoggedIn(ServerPlayer player) {
        UUID uuid = player.getUUID();

        // 假人登录处理
        FakePlayerManager.getInstance().onPlayerLoggedIn(player);

        // 实体优化：发送当前隐藏实体列表
        if (EntityOptimizationManager.getInstance() != null) {
            ServerLevel level = player.level();
            HiddenEntityTracker tracker = EntityOptimizationManager.getInstance().getTracker(level);
            if (tracker != null) {
                IntSet hiddenIds = tracker.getCurrentHiddenIds();
                int[] added = hiddenIds.toIntArray();
                HideEntitiesPayload payload = new HideEntitiesPayload(added, new int[0]);
                ServerPlayNetworking.send(player, payload);
            }
        }

        // 发送 command-gui 服务端预设指令给客户端
        ServerCommandConfigManager.sendToPlayer(player);

        playerDimensions.put(player.getUUID(), player.level().dimension());
    }

    public void onPlayerLoggedOut(ServerPlayer player) {
        UUID uuid = player.getUUID();
        FakePlayerManager.getInstance().onPlayerLoggedOut(player);
        HighlightManager.getInstance().onPlayerLoggedOut(uuid);
        playerDimensions.remove(uuid);
    }

    public void tick(MinecraftServer server) {
        // 检测玩家维度变化并通知实体优化
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ResourceKey<Level> currentDim = player.level().dimension();
            ResourceKey<Level> previousDim = playerDimensions.get(player.getUUID());
            if (previousDim != null && !previousDim.equals(currentDim)) {
                if (EntityOptimizationManager.getInstance() != null) {
                    ServerLevel level = player.level();
                    HiddenEntityTracker tracker = EntityOptimizationManager.getInstance().getTracker(level);
                    if (tracker != null) {
                        IntSet hiddenIds = tracker.getCurrentHiddenIds();
                        int[] added = hiddenIds.toIntArray();
                        HideEntitiesPayload payload = new HideEntitiesPayload(added, new int[0]);
                        ServerPlayNetworking.send(player, payload);
                    }
                }
                playerDimensions.put(player.getUUID(), currentDim);
            }
        }
    }

    public void onServerClosed() {
        playerDimensions.clear();
    }
}