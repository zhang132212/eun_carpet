package com.eun.carpet.aioptimization;

import com.eun.carpet.EUNCarpetMod;
import com.eun.carpet.config.EUNConfigManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class AIOptimizationConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(EUNCarpetMod.MOD_ID + "|AIOptConfig");
    private static final Path CONFIG_FILE = EUNConfigManager.getConfigRoot().resolve("ai_optimization.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public List<String> enabledTypes = new ArrayList<>(List.of("minecraft:zombie", "minecraft:piglin", "minecraft:skeleton"));
    public int minStackSize = 24;
    public int checkInterval = 100;
    public double positionChangeThreshold = 0.5;

    private transient Set<EntityType<?>> enabledEntityTypes = null;

    private static AIOptimizationConfig instance;

    public static AIOptimizationConfig getInstance() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        if (Files.exists(CONFIG_FILE)) {
            try {
                String json = Files.readString(CONFIG_FILE);
                instance = GSON.fromJson(json, AIOptimizationConfig.class);
                if (instance == null) instance = new AIOptimizationConfig();
                if (instance.enabledTypes == null) instance.enabledTypes = new ArrayList<>();
            } catch (IOException e) {
                LOGGER.error("Failed to load AI optimization config", e);
                instance = new AIOptimizationConfig();
            }
        } else {
            instance = new AIOptimizationConfig();
            instance.save();
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            Files.writeString(CONFIG_FILE, GSON.toJson(this));
        } catch (IOException e) {
            LOGGER.error("Failed to save AI optimization config", e);
        }
    }

    public static void notifyUpdated() {
        load();
        if (AIOptimizationManager.getInstance() != null) {
            AIOptimizationManager.getInstance().reloadConfig();
        }
    }

    public Set<EntityType<?>> getEnabledEntityTypes() {
        if (enabledEntityTypes == null) {
            enabledEntityTypes = enabledTypes.stream()
                    .map(Identifier::tryParse)          // 使用 Identifier::tryParse
                    .filter(Objects::nonNull)
                    .flatMap(id -> BuiltInRegistries.ENTITY_TYPE.getOptional(id).stream())
                    .collect(Collectors.toSet());
        }
        return enabledEntityTypes;
    }

    public int getMinStackSize() { return minStackSize; }
    public int getCheckInterval() { return checkInterval; }
    public double getPositionChangeThreshold() { return positionChangeThreshold; }
}