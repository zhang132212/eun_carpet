package com.eun.carpet.highlight;

import com.eun.carpet.EUNCarpetSettings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

/**
 * 全局高亮实现(纯服务端, 无需客户端mod):
 * 利用原版荧光机制 entity.setGlowingTag(true) + 白色队伍 渲染实体发光轮廓。
 * 统一白色, 全局生效(发光对所有玩家可见)。
 */
public class HighlightManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("EUNCarpet|Highlight");
    private static final String TEAM_NAME = "eun_hl_white";

    private static HighlightManager instance;
    private final MinecraftServer server;
    private String lastEnabledState = EUNCarpetSettings.highlightEnabled;

    private boolean highlightItems = false;
    private boolean highlightEntities = false;

    private HighlightManager(MinecraftServer server) {
        this.server = server;
    }

    public static void init(MinecraftServer server) {
        instance = new HighlightManager(server);
    }

    public static HighlightManager getInstance() {
        return instance;
    }

    /** 全局设置: /eun highlight item|entity true|false */
    public void setGlobal(boolean items, boolean entities) {
        highlightItems = items;
        highlightEntities = entities;
        LOGGER.info("[Highlight] 全局设置: items={} entities={}", items, entities);
    }

    public boolean isHighlightItems() {
        return highlightItems;
    }

    public boolean isHighlightEntities() {
        return highlightEntities;
    }

    public void tick(MinecraftServer server) {
        String currentEnabled = EUNCarpetSettings.highlightEnabled;
        if (!lastEnabledState.equals(currentEnabled)) {
            lastEnabledState = currentEnabled;
            if (currentEnabled.equals("false")) {
                highlightItems = false;
                highlightEntities = false;
                clearAllGlow(server);
            }
        }
        if (currentEnabled.equals("false")) return;

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                applyGlow(level, entity);
            }
        }
    }

    private void applyGlow(ServerLevel level, Entity entity) {
        boolean shouldGlow = false;
        if (highlightItems && entity instanceof ItemEntity) {
            shouldGlow = true;
        } else if (highlightEntities) {
            if ((entity instanceof LivingEntity && !(entity instanceof Player)) || entity instanceof AbstractMinecart) {
                shouldGlow = true;
            }
        }

        if (shouldGlow) {
            if (!entity.hasGlowingTag()) entity.setGlowingTag(true);
            joinWhiteTeam(level, entity);
        } else {
            if (entity.hasGlowingTag()) entity.setGlowingTag(false);
            leaveTeam(level, entity);
        }
    }

    private void joinWhiteTeam(ServerLevel level, Entity entity) {
        Scoreboard scoreboard = level.getServer().getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_NAME);
        if (team == null) {
            team = scoreboard.addPlayerTeam(TEAM_NAME);
            team.setColor(Optional.of(TeamColor.WHITE));
            team.setNameTagVisibility(PlayerTeam.Visibility.NEVER);
        }
        if (scoreboard.getPlayerTeam(entity.getStringUUID()) != team) {
            scoreboard.addPlayerToTeam(entity.getStringUUID(), team);
        }
    }

    private void leaveTeam(ServerLevel level, Entity entity) {
        Scoreboard scoreboard = level.getServer().getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(entity.getStringUUID());
        if (team != null && team.getName().equals(TEAM_NAME)) {
            scoreboard.removePlayerFromTeam(entity.getStringUUID());
        }
    }

    private void clearAllGlow(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity.hasGlowingTag()) entity.setGlowingTag(false);
                leaveTeam(level, entity);
            }
        }
    }

    public void onPlayerLoggedOut(UUID playerUuid) {
        // 全局模式无需按玩家清理
    }

    public void onServerClosed() {
        highlightItems = false;
        highlightEntities = false;
    }
}
