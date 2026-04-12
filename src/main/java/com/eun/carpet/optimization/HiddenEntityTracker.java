package com.eun.carpet.optimization;

import com.eun.carpet.EUNCarpetSettings;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class HiddenEntityTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger("EUNCarpet|Debug");

    private final ServerLevel level;
    private final Map<BlockPosTypeKey, AreaState> areaStates = new HashMap<>();
    private final IntSet currentHiddenIds = new IntOpenHashSet();
    private final List<EntityUpdate> pendingAdds = new ArrayList<>();
    private final List<EntityUpdate> pendingRemoves = new ArrayList<>();
    private int sendCooldown = 0;

    public HiddenEntityTracker(ServerLevel level) {
        this.level = level;
    }

    public IntSet getCurrentHiddenIds() {
        return currentHiddenIds;
    }

    public void tick() {
        if (sendCooldown > 0) {
            sendCooldown--;
        }
        if (sendCooldown == 0 && (!pendingAdds.isEmpty() || !pendingRemoves.isEmpty())) {
            flushUpdates();
        }
    }

    public void scan() {
        if (!EUNCarpetSettings.entityOptimizationEnabled) {
            if (!currentHiddenIds.isEmpty()) {
                for (int id : currentHiddenIds) {
                    pendingRemoves.add(new EntityUpdate(id, false));
                }
                currentHiddenIds.clear();
                flushUpdates();
            }
            return;
        }

        EntityOptimizationConfig config = EntityOptimizationConfig.getInstance();

        Map<BlockPosTypeKey, List<Entity>> groups = new HashMap<>();
        for (Entity entity : level.getAllEntities()) {
            if (shouldSkip(entity, config)) continue;
            BlockPos pos = entity.blockPosition();
            BlockPosTypeKey key = new BlockPosTypeKey(pos, entity.getType());
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(entity);
        }

        IntSet newHiddenIds = new IntOpenHashSet();
        for (Map.Entry<BlockPosTypeKey, List<Entity>> entry : groups.entrySet()) {
            BlockPosTypeKey key = entry.getKey();
            List<Entity> entities = entry.getValue();
            int rawCount = entities.size();

            if (rawCount <= config.getMinStackSize()) {
                continue;
            }

            AreaState state = areaStates.computeIfAbsent(key, k -> new AreaState());
            state.updateSmoothCount(rawCount, config.getSmoothingAlpha());

            int keep = calculateKeep(state.smoothCount, config.getKeepFormula());
            int hide = rawCount - keep;

            if (hide <= 0) continue;

            entities.sort(Comparator.comparingInt(Entity::getId));
            for (int i = 0; i < hide; i++) {
                newHiddenIds.add(entities.get(i).getId());
            }

            if (config.isDebug()) {
                LOGGER.info("[EUN Debug] 扫描组 {}: 原始数量={}, 平滑后={:.2f}, 保留={}, 隐藏={}",
                        key.getType().getDescription().getString(),
                        rawCount, state.smoothCount, keep, hide);
            }
        }

        if (currentHiddenIds.isEmpty()) {
            if (!newHiddenIds.isEmpty()) {
                newHiddenIds.forEach(id -> pendingAdds.add(new EntityUpdate(id, true)));
                currentHiddenIds.addAll(newHiddenIds);
            }
        } else {
            IntSet added = new IntOpenHashSet(newHiddenIds);
            added.removeAll(currentHiddenIds);
            IntSet removed = new IntOpenHashSet(currentHiddenIds);
            removed.removeAll(newHiddenIds);

            if (!added.isEmpty() || !removed.isEmpty()) {
                int totalActive = currentHiddenIds.size();
                double changeRatio = (added.size() + removed.size()) / (double) (totalActive + 1);
                double threshold = config.getHysteresisThreshold() + 5.0 / (totalActive + 1);

                if (changeRatio > threshold) {
                    added.forEach(id -> pendingAdds.add(new EntityUpdate(id, true)));
                    removed.forEach(id -> pendingRemoves.add(new EntityUpdate(id, false)));
                    currentHiddenIds.clear();
                    currentHiddenIds.addAll(newHiddenIds);
                }
            }
        }
    }

    private boolean shouldSkip(Entity entity, EntityOptimizationConfig config) {
        if (entity instanceof Player) return true;
        if (entity.hasCustomName()) return true;
        if (entity.isVehicle()) return true;
        if (entity instanceof EnderDragon || entity instanceof WitherBoss || entity instanceof EnderDragonPart) return true;

        List<String> excluded = config.getExcludedTypes();
        if (!excluded.isEmpty()) {
            String typeKey = EntityType.getKey(entity.getType()).toString();
            for (String ex : excluded) {
                if (ex.trim().equals(typeKey)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int calculateKeep(double smoothCount, String formula) {
        return switch (formula) {
            case "sqrt" -> (int) Math.ceil(Math.sqrt(smoothCount));
            case "log2" -> (int) Math.ceil(Math.log(smoothCount) / Math.log(2));
            case "linear" -> (int) Math.ceil(smoothCount * 0.3);
            default -> (int) Math.ceil(Math.sqrt(smoothCount));
        };
    }

    private void flushUpdates() {
        if (pendingAdds.isEmpty() && pendingRemoves.isEmpty()) return;

        int[] addedArray = pendingAdds.stream().filter(u -> u.added).mapToInt(u -> u.id).toArray();
        int[] removedArray = pendingRemoves.stream().filter(u -> !u.added).mapToInt(u -> u.id).toArray();

        HideEntitiesPayload payload = new HideEntitiesPayload(addedArray, removedArray);
        for (ServerPlayer player : level.players()) {
            if (player.connection != null) {
                ServerPlayNetworking.send(player, payload);
            }
        }

        EntityOptimizationConfig config = EntityOptimizationConfig.getInstance();
        if (config.isDebug()) {
            LOGGER.info("[EUN Debug] 发送更新包: 添加 {} 个实体 {}, 移除 {} 个实体 {}",
                    addedArray.length, Arrays.toString(addedArray),
                    removedArray.length, Arrays.toString(removedArray));
        }

        pendingAdds.clear();
        pendingRemoves.clear();
        sendCooldown = config.getSendInterval();
    }

    private static class AreaState {
        double smoothCount = 0;
        void updateSmoothCount(int raw, double alpha) {
            if (smoothCount == 0) {
                smoothCount = raw;
            } else {
                smoothCount = alpha * raw + (1 - alpha) * smoothCount;
            }
        }
    }

    private record EntityUpdate(int id, boolean added) {}
}