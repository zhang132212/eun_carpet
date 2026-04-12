package com.eun.carpet.scoreboard;

import com.eun.carpet.EUNCarpetSettings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import carpet.patches.EntityPlayerMPFake;

import java.util.*;

public class GlobalScoreboardManager {
    private static GlobalScoreboardManager instance;
    private final MinecraftServer server;
    private int tickCounter = 0;
    private int displayIndex = 0;
    private static final String OBJECTIVE_PREFIX = "eun_";

    private GlobalScoreboardManager(MinecraftServer server) {
        this.server = server;
        createObjectives();
    }

    public static void init(MinecraftServer server) {
        instance = new GlobalScoreboardManager(server);
    }

    public static GlobalScoreboardManager getInstance() {
        return instance;
    }

    private void createObjectives() {
        Scoreboard scoreboard = server.getScoreboard();
        for (ScoreBoardType type : ScoreBoardType.values()) {
            String name = OBJECTIVE_PREFIX + type.name().toLowerCase(Locale.ROOT);
            if (scoreboard.getObjective(name) == null) {
                String displayName = type.displayName + "榜" + getIconSuffix(type);
                scoreboard.addObjective(
                        name,
                        ObjectiveCriteria.DUMMY,
                        net.minecraft.network.chat.Component.literal(displayName),
                        ObjectiveCriteria.RenderType.INTEGER,
                        false,
                        null
                );
            }
        }
    }

    private String getIconSuffix(ScoreBoardType type) {
        String icon = switch (type) {
            case MINING -> "⛏";
            case BUILDING -> "🏗";
            case KILL -> "🗡";
            case DAMAGE_DEALT -> "💥";
            case DAMAGE_TAKEN -> "🛡";
            case FLY -> "🦅";
            case TRADE -> "💰";
            case DEATH -> "💀";
            case FISH -> "🎣";
            case PLAY_TIME -> "⏳(h)"; // 注明单位
        };
        return "(" + icon + ")";
    }

    public void tick() {
        if (!EUNCarpetSettings.globalScoreboardEnabled) {
            clearDisplay();
            return;
        }

        tickCounter++;
        if (tickCounter >= 200) { // 每10秒更新分数
            tickCounter = 0;
            updateScores();
        }
        if (tickCounter % 100 == 0) { // 每5秒轮播
            rotateDisplay();
        }
    }

    private void updateScores() {
        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player instanceof EntityPlayerMPFake)) {
                players.add(player);
            }
        }
        if (players.isEmpty()) return;

        Scoreboard scoreboard = server.getScoreboard();

        for (ScoreBoardType type : ScoreBoardType.values()) {
            String objName = OBJECTIVE_PREFIX + type.name().toLowerCase(Locale.ROOT);
            Objective objective = scoreboard.getObjective(objName);
            if (objective == null) continue;

            for (ServerPlayer player : players) {
                int rawValue = type.getValue(player.getStats());
                int displayValue = rawValue; // 默认不变

                // 对在线时长特殊处理：tick -> 小时*100
                if (type == ScoreBoardType.PLAY_TIME) {
                    displayValue = (int) Math.round(rawValue * 100.0 / 72000.0);
                }

                scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(player.getScoreboardName()), objective).set(displayValue);
            }
        }
    }

    private void rotateDisplay() {
        ScoreBoardType[] types = ScoreBoardType.values();
        if (types.length == 0) return;
        displayIndex = (displayIndex + 1) % types.length;
        String objName = OBJECTIVE_PREFIX + types[displayIndex].name().toLowerCase(Locale.ROOT);
        Objective objective = server.getScoreboard().getObjective(objName);
        if (objective != null) {
            server.getScoreboard().setDisplayObjective(DisplaySlot.SIDEBAR, objective);
        }
    }

    private void clearDisplay() {
        server.getScoreboard().setDisplayObjective(DisplaySlot.SIDEBAR, null);
    }
}