package com.eun.carpet.highlight;

import com.eun.carpet.EUNCarpetSettings;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HighlightManager {
    private static HighlightManager instance;
    private final MinecraftServer server;
    private final Map<UUID, HighlightSettings> playerSettings = new ConcurrentHashMap<>();
    private String lastEnabledState = EUNCarpetSettings.highlightEnabled;

    private HighlightManager(MinecraftServer server) {
        this.server = server;
    }

    public static void init(MinecraftServer server) {
        instance = new HighlightManager(server);
    }

    public static HighlightManager getInstance() {
        return instance;
    }

    public void tick(MinecraftServer server) {
        String currentEnabled = EUNCarpetSettings.highlightEnabled;
        if (!lastEnabledState.equals(currentEnabled)) {
            lastEnabledState = currentEnabled;
            boolean isEnabled = !currentEnabled.equals("false");
            if (!isEnabled) {
                // 总开关关闭：向所有在线玩家发送关闭高亮 payload
                HighlightPayload closePayload = new HighlightPayload(false, false, 0xFFFFFFFF, 0xFFFFFFFF);
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(player, closePayload);
                }
            } else {
                // 总开关打开：根据存储的设置恢复每个玩家
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    HighlightSettings settings = playerSettings.get(player.getUUID());
                    if (settings != null) {
                        HighlightPayload payload = new HighlightPayload(settings.items, settings.entities, settings.itemColor, settings.entityColor);
                        ServerPlayNetworking.send(player, payload);
                    }
                }
            }
        }
    }

    public void onPlayerLoggedOut(UUID playerUuid) {
        playerSettings.remove(playerUuid);
    }

    public void onServerClosed() {
        playerSettings.clear();
    }

    public void setPlayerSettings(UUID playerUuid, boolean items, boolean entities, int itemColor, int entityColor) {
        HighlightSettings settings = playerSettings.computeIfAbsent(playerUuid, k -> new HighlightSettings());
        settings.items = items;
        settings.entities = entities;
        settings.itemColor = itemColor;
        settings.entityColor = entityColor;
    }

    public HighlightSettings getPlayerSettings(UUID playerUuid) {
        return playerSettings.get(playerUuid);
    }

    public static class HighlightSettings {
        public boolean items = false;
        public boolean entities = false;
        public int itemColor = 0xFFFFFFFF;
        public int entityColor = 0xFFFFFFFF;
    }
}