package com.eun.carpet.fakeplayer;

import com.eun.carpet.EUNCarpetSettings;
import com.eun.carpet.fakeplayer.FakePlayerData;
import com.eun.carpet.fakeplayer.FakePlayerPersistence;
import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FakePlayerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("EUNCarpet|FakePlayer");
    private static FakePlayerManager instance;
    private final MinecraftServer server;

    private final Map<UUID, Integer> lastLogoutTick = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> blacklist = new ConcurrentHashMap<>();
    private final Map<UUID, FakePlayerActionSnapshot> lastSavedActionSnapshot = new ConcurrentHashMap<>();

    // 反射字段
    private static final Field actionPackActionsField;
    private static final Field actionDoneField;
    private static final Field actionIsContinuousField;
    private static final Field actionLimitField;
    private static final Field actionIntervalField;
    private static final Field actionOffsetField;
    private static final Field actionPackForwardField;
    private static final Field actionPackStrafingField;

    static {
        try {
            actionPackActionsField = EntityPlayerActionPack.class.getDeclaredField("actions");
            actionPackActionsField.setAccessible(true);
            actionPackForwardField = EntityPlayerActionPack.class.getDeclaredField("forward");
            actionPackForwardField.setAccessible(true);
            actionPackStrafingField = EntityPlayerActionPack.class.getDeclaredField("strafing");
            actionPackStrafingField.setAccessible(true);

            Class<?> actionClass = EntityPlayerActionPack.Action.class;
            actionDoneField = actionClass.getDeclaredField("done");
            actionDoneField.setAccessible(true);
            actionIsContinuousField = actionClass.getDeclaredField("isContinuous");
            actionIsContinuousField.setAccessible(true);
            actionLimitField = actionClass.getDeclaredField("limit");
            actionLimitField.setAccessible(true);
            actionIntervalField = actionClass.getDeclaredField("interval");
            actionIntervalField.setAccessible(true);
            actionOffsetField = actionClass.getDeclaredField("offset");
            actionOffsetField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to initialize reflection fields for EntityPlayerActionPack", e);
        }
    }

    private FakePlayerManager(MinecraftServer server) {
        this.server = server;
    }

    public static void init(MinecraftServer server) {
        instance = new FakePlayerManager(server);
    }

    public static FakePlayerManager getInstance() {
        return instance;
    }

    public void summonFakePlayers(MinecraftServer server) {
        if (!EUNCarpetSettings.fakePlayerPersistence) return;

        for (FakePlayerData data : FakePlayerPersistence.getAll()) {
            UUID uuid = data.getUuid();
            if (blacklist.containsKey(uuid)) continue;
            if (server.getPlayerList().getPlayer(uuid) != null) continue;

            boolean flying = data.getGameMode() == GameType.CREATIVE || data.getGameMode() == GameType.SPECTATOR;
            EntityPlayerMPFake.createFake(
                    data.getName(),
                    server,
                    data.getPosition(),
                    data.getYaw(),
                    data.getPitch(),
                    data.getDimension(),
                    data.getGameMode(),
                    flying
            );
        }
    }

    public void onPlayerLoggedIn(ServerPlayer player) {
        if (!(player instanceof EntityPlayerMPFake fakePlayer)) return;

        UUID uuid = player.getUUID();
        int currentTick = server.getTickCount();

        if (EUNCarpetSettings.fakePlayerPersistence) {
            Integer lastLogout = lastLogoutTick.get(uuid);
            if (lastLogout != null && currentTick - lastLogout <= 20) {
                blacklist.put(uuid, 60);
            }

            FakePlayerData existingData = FakePlayerPersistence.get(uuid);
            if (existingData == null) {
                // 首次登录，创建数据
                server.execute(() -> server.execute(() -> {
                    if (FakePlayerPersistence.get(uuid) == null) {
                        Vec3 pos = player.position();
                        GameType gameMode = player.gameMode.getGameModeForPlayer();
                        FakePlayerData newData = new FakePlayerData(
                                uuid,
                                player.getName().getString(),
                                pos,
                                player.getYRot(),
                                player.getXRot(),
                                gameMode,
                                player.level().dimension()
                        );
                        FakePlayerPersistence.put(newData);
                        FakePlayerData current = captureCurrentFakePlayerData(fakePlayer);
                        lastSavedActionSnapshot.put(uuid, new FakePlayerActionSnapshot(
                                current.isSneaking(),
                                current.isSprinting(),
                                current.getForward(),
                                current.getStrafing(),
                                current.getActions()
                        ));
                    }
                }));
            } else {
                if (EUNCarpetSettings.fakePlayerActionSaving) {
                    restoreFakePlayerActions(fakePlayer, existingData);
                }
                FakePlayerData current = captureCurrentFakePlayerData(fakePlayer);
                lastSavedActionSnapshot.put(uuid, new FakePlayerActionSnapshot(
                        current.isSneaking(),
                        current.isSprinting(),
                        current.getForward(),
                        current.getStrafing(),
                        current.getActions()
                ));
            }
        }

        if (EUNCarpetSettings.fakePlayerPrefix) {
            addFakePlayerToTeam(fakePlayer);
        }
    }

    public void onPlayerLoggedOut(ServerPlayer player) {
        if (!(player instanceof EntityPlayerMPFake)) return;

        UUID uuid = player.getUUID();
        lastSavedActionSnapshot.remove(uuid);

        if (!EUNCarpetSettings.fakePlayerPersistence) return;
        if (server.isStopped()) return;

        lastLogoutTick.put(uuid, server.getTickCount());
        FakePlayerPersistence.remove(uuid);
    }

    public void tick(MinecraftServer server) {
        if (!blacklist.isEmpty()) {
            blacklist.replaceAll((uuid, ticks) -> ticks - 1);
            blacklist.entrySet().removeIf(entry -> entry.getValue() <= 0);
        }

        // 每30秒保存一次假人动作
        if (server.getTickCount() % 600 == 0 && EUNCarpetSettings.fakePlayerActionSaving) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player instanceof EntityPlayerMPFake fakePlayer) {
                    UUID uuid = fakePlayer.getUUID();
                    FakePlayerData currentData = captureCurrentFakePlayerData(fakePlayer);
                    FakePlayerActionSnapshot lastSnapshot = lastSavedActionSnapshot.get(uuid);
                    if (lastSnapshot == null || !lastSnapshot.equals(currentData)) {
                        FakePlayerData storedData = FakePlayerPersistence.get(uuid);
                        if (storedData != null) {
                            storedData.updateActionsFrom(currentData);
                            FakePlayerPersistence.put(storedData);
                        }
                        lastSavedActionSnapshot.put(uuid, new FakePlayerActionSnapshot(
                                currentData.isSneaking(),
                                currentData.isSprinting(),
                                currentData.getForward(),
                                currentData.getStrafing(),
                                currentData.getActions()
                        ));
                    }
                }
            }
        }

    }

    public void onServerClosed(MinecraftServer server) {
        if (EUNCarpetSettings.fakePlayerActionSaving) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player instanceof EntityPlayerMPFake fakePlayer) {
                    FakePlayerData currentData = captureCurrentFakePlayerData(fakePlayer);
                    FakePlayerData storedData = FakePlayerPersistence.get(fakePlayer.getUUID());
                    if (storedData != null) {
                        storedData.updateActionsFrom(currentData);
                        FakePlayerPersistence.put(storedData);
                    }
                }
            }
        }
        lastLogoutTick.clear();
        blacklist.clear();
        lastSavedActionSnapshot.clear();
    }

    // 假人前缀处理
    public void applyPrefixToAllFakePlayers(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player instanceof EntityPlayerMPFake) {
                addFakePlayerToTeam((EntityPlayerMPFake) player);
            }
        }
    }

    public void removePrefixFromAllFakePlayers(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam("EUN_FakePlayers");
        if (team != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player instanceof EntityPlayerMPFake) {
                    scoreboard.removePlayerFromTeam(player.getScoreboardName());
                }
            }
        }
    }

    private void addFakePlayerToTeam(EntityPlayerMPFake fakePlayer) {
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam("EUN_FakePlayers");
        if (team == null) {
            team = scoreboard.addPlayerTeam("EUN_FakePlayers");
            team.setPlayerPrefix(net.minecraft.network.chat.Component.literal("[假人] "));
        }
        if (!team.getPlayers().contains(fakePlayer.getScoreboardName())) {
            scoreboard.addPlayerToTeam(fakePlayer.getScoreboardName(), team);
        }
    }

    // 动作保存与恢复
    private void restoreFakePlayerActions(EntityPlayerMPFake fakePlayer, FakePlayerData data) {
        EntityPlayerActionPack actionPack = ((ServerPlayerInterface) fakePlayer).getActionPack();

        actionPack.setSneaking(data.isSneaking());
        actionPack.setSprinting(data.isSprinting());

        try {
            actionPackForwardField.setFloat(actionPack, data.getForward());
            actionPackStrafingField.setFloat(actionPack, data.getStrafing());
        } catch (IllegalAccessException e) {
            LOGGER.error("Failed to restore forward/strafing", e);
        }

        if (data.getActions() != null) {
            for (FakePlayerData.SavedAction saved : data.getActions()) {
                try {
                    EntityPlayerActionPack.ActionType type = EntityPlayerActionPack.ActionType.valueOf(saved.getType());
                    EntityPlayerActionPack.Action action;
                    if (saved.isContinuous()) {
                        action = EntityPlayerActionPack.Action.continuous();
                    } else if (saved.getInterval() > 0) {
                        action = EntityPlayerActionPack.Action.interval(saved.getInterval(), saved.getOffset());
                    } else {
                        action = EntityPlayerActionPack.Action.once();
                    }
                    actionPack.start(type, action);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Unknown action type: {}", saved.getType());
                }
            }
        }
    }

    private FakePlayerData captureCurrentFakePlayerData(EntityPlayerMPFake fakePlayer) {
        UUID uuid = fakePlayer.getUUID();
        FakePlayerData data = new FakePlayerData(
                uuid,
                fakePlayer.getName().getString(),
                fakePlayer.position(),
                fakePlayer.getYRot(),
                fakePlayer.getXRot(),
                fakePlayer.gameMode.getGameModeForPlayer(),
                fakePlayer.level().dimension()
        );

        EntityPlayerActionPack actionPack = ((ServerPlayerInterface) fakePlayer).getActionPack();

        data.setSneaking(fakePlayer.isShiftKeyDown());
        data.setSprinting(fakePlayer.isSprinting());

        try {
            data.setForward(actionPackForwardField.getFloat(actionPack));
            data.setStrafing(actionPackStrafingField.getFloat(actionPack));
        } catch (IllegalAccessException e) {
            LOGGER.error("Failed to capture forward/strafing", e);
        }

        List<FakePlayerData.SavedAction> savedActions = new ArrayList<>();
        try {
            Map<EntityPlayerActionPack.ActionType, EntityPlayerActionPack.Action> actions =
                    (Map<EntityPlayerActionPack.ActionType, EntityPlayerActionPack.Action>) actionPackActionsField.get(actionPack);
            if (actions != null) {
                for (Map.Entry<EntityPlayerActionPack.ActionType, EntityPlayerActionPack.Action> entry : actions.entrySet()) {
                    EntityPlayerActionPack.Action action = entry.getValue();
                    boolean done = actionDoneField.getBoolean(action);
                    if (done) continue;
                    boolean isContinuous = actionIsContinuousField.getBoolean(action);
                    int limit = actionLimitField.getInt(action);
                    int interval = actionIntervalField.getInt(action);
                    int offset = actionOffsetField.getInt(action);
                    savedActions.add(new FakePlayerData.SavedAction(
                            entry.getKey().name(),
                            limit,
                            interval,
                            offset,
                            isContinuous
                    ));
                }
            }
        } catch (IllegalAccessException e) {
            LOGGER.error("Failed to capture actions", e);
        }
        data.setActions(savedActions);
        return data;
    }

    // 内部快照类
    private record FakePlayerActionSnapshot(boolean sneaking, boolean sprinting, float forward, float strafing,
                                            List<FakePlayerData.SavedAction> actions) {
        private FakePlayerActionSnapshot(boolean sneaking, boolean sprinting, float forward, float strafing,
                                         List<FakePlayerData.SavedAction> actions) {
            this.sneaking = sneaking;
            this.sprinting = sprinting;
            this.forward = forward;
            this.strafing = strafing;
            this.actions = actions != null ? new ArrayList<>(actions) : null;
        }

        public boolean equals(FakePlayerData data) {
            if (sneaking != data.isSneaking()) return false;
            if (sprinting != data.isSprinting()) return false;
            if (Float.compare(forward, data.getForward()) != 0) return false;
            if (Float.compare(strafing, data.getStrafing()) != 0) return false;
            if (actions == null && data.getActions() == null) return true;
            if (actions == null || data.getActions() == null) return false;
            return actions.equals(data.getActions());
        }
    }
}