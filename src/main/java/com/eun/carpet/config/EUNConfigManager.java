package com.eun.carpet.config;

import com.eun.carpet.EUNCarpetMod;
import com.eun.carpet.aioptimization.AIOptimizationConfig;
import com.eun.carpet.optimization.EntityOptimizationConfig;
import com.eun.carpet.packet.PacketManager;
import com.eun.carpet.pearlcannon.PearlCannonManager;
import com.eun.carpet.preset.PresetManager;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class EUNConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EUNCarpetMod.MOD_ID + "|Config");
    private static final Path CONFIG_ROOT = FabricLoader.getInstance().getConfigDir().resolve("eun_carpet");

    public static void loadAllConfigs() {
        ensureDirectories();
        EntityOptimizationConfig.load();
        AIOptimizationConfig.load();
        PearlCannonManager.getInstance().reload();
        PacketManager.load();
        PresetManager.load();  // 首次加载
        LOGGER.info("All EUN configurations loaded.");
    }

    public static void reloadAllConfigs() {
        EntityOptimizationConfig.load();
        AIOptimizationConfig.load();
        PearlCannonManager.getInstance().reload();
        PacketManager.load();
        PresetManager.reload(); // 重载预设
        EntityOptimizationConfig.notifyUpdated();
        AIOptimizationConfig.notifyUpdated();
        LOGGER.info("All EUN configurations reloaded.");
    }

    private static void ensureDirectories() {
        try {
            if (!java.nio.file.Files.exists(CONFIG_ROOT)) {
                java.nio.file.Files.createDirectories(CONFIG_ROOT);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create config root directory", e);
        }
    }

    public static Path getConfigRoot() {
        return CONFIG_ROOT;
    }
}