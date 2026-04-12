package com.eun.carpet.client.optimization;

import com.eun.carpet.client.config.ModConfig;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class ClientTransparencyManager {
    private static ClientTransparencyManager instance;
    private final IntSet transparentIds = new IntOpenHashSet();
    private final IntSet pendingTransparentIds = new IntOpenHashSet();

    private ClientTransparencyManager() {}

    public static ClientTransparencyManager getInstance() {
        if (instance == null) instance = new ClientTransparencyManager();
        return instance;
    }

    public void update(int[] added, int[] removed) {
        Minecraft.getInstance().execute(() -> {
            for (int id : added) {
                transparentIds.add(id);
                pendingTransparentIds.remove(id);
            }
            for (int id : removed) {
                transparentIds.remove(id);
            }
        });
    }

    public void onEntityAdded(Entity entity) {
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        if (!config.enableOptimization) return;
        if (config.excludedEntityTypes.contains(EntityType.getKey(entity.getType()).toString())) {
            return;
        }
        if (pendingTransparentIds.contains(entity.getId())) {
            transparentIds.add(entity.getId());
            pendingTransparentIds.remove(entity.getId());
        }
    }

    public void markPending(int id) {
        pendingTransparentIds.add(id);
    }

    public boolean shouldHide(int entityId) {
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        return config.enableOptimization && transparentIds.contains(entityId);
    }

    public void clear() {
        transparentIds.clear();
        pendingTransparentIds.clear();
    }
}