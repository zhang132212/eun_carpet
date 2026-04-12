package com.eun.carpet.optimization;

import com.eun.carpet.EUNCarpetMod;
import com.eun.carpet.config.EUNConfigManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class EntityOptimizationConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(EUNCarpetMod.MOD_ID + "|EntityOptConfig");
    private static final Path CONFIG_FILE = EUNConfigManager.getConfigRoot().resolve("entity_optimization.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int minStackSize = 24;
    public String keepFormula = "sqrt";
    public double smoothingAlpha = 0.3;
    public double hysteresisThreshold = 0.1;
    public int sendInterval = 10;
    public List<String> excludedTypes = new ArrayList<>();
    public boolean debug = false;

    private static EntityOptimizationConfig instance;

    public static EntityOptimizationConfig getInstance() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        if (Files.exists(CONFIG_FILE)) {
            try {
                String json = Files.readString(CONFIG_FILE);
                instance = GSON.fromJson(json, EntityOptimizationConfig.class);
                if (instance == null) instance = new EntityOptimizationConfig();
            } catch (IOException e) {
                LOGGER.error("Failed to load entity optimization config, using defaults", e);
                instance = new EntityOptimizationConfig();
            }
        } else {
            instance = new EntityOptimizationConfig();
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            Files.writeString(CONFIG_FILE, GSON.toJson(instance));
        } catch (IOException e) {
            LOGGER.error("Failed to save entity optimization config", e);
        }
    }

    public static void notifyUpdated() {
        // 配置已从文件重新加载，各tracker下次scan时会使用新值
    }

    public int getMinStackSize() { return minStackSize; }
    public String getKeepFormula() { return keepFormula; }
    public double getSmoothingAlpha() { return smoothingAlpha; }
    public double getHysteresisThreshold() { return hysteresisThreshold; }
    public int getSendInterval() { return sendInterval; }
    public List<String> getExcludedTypes() { return excludedTypes; }
    public boolean isDebug() { return debug; }
}