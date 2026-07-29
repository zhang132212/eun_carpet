package com.eun.carpet;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import com.eun.carpet.command.EUNCommands;
import com.eun.carpet.config.EUNConfigManager;
import com.eun.carpet.fakeplayer.FakePlayerManager;
import com.eun.carpet.highlight.HighlightManager;
import com.eun.carpet.listener.PlayerEventListener;
import com.eun.carpet.optimization.EntityOptimizationManager;
import com.eun.carpet.packet.PacketManager;
import com.eun.carpet.recipe.InvisibleFrameManager;
import com.eun.carpet.scoreboard.GlobalScoreboardManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

public class EUNCarpetExtension implements CarpetExtension {
    private static final Logger LOGGER = LoggerFactory.getLogger("EUNCarpet");

    private boolean worldsLoaded = false;
    private boolean serverStarted = false;
    private boolean summoned = false;

    private String lastPearlCannonEnabled = EUNCarpetSettings.pearlCannonEnabled;
    private String lastHighlightEnabled = EUNCarpetSettings.highlightEnabled;
    private String lastPacketEnabled = EUNCarpetSettings.packetEnabled;
    private boolean lastCraftableInvisibleItemFrames = EUNCarpetSettings.craftableInvisibleItemFrames;
    private boolean lastFakePlayerPrefix = EUNCarpetSettings.fakePlayerPrefix;

    private CommandDispatcher<CommandSourceStack> cachedDispatcher;
    private CommandBuildContext cachedBuildContext;

    @Override
    public void onGameStarted() {
        CarpetServer.settingsManager.parseSettingsClass(EUNCarpetSettings.class);
        EUNConfigManager.loadAllConfigs();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverStarted = true;
            if (worldsLoaded && !summoned) {
                FakePlayerManager.getInstance().summonFakePlayers(server);
                summoned = true;
            }
        });
    }

    @Override
    public void onServerLoaded(MinecraftServer server) {}

    @Override
    public void onServerLoadedWorlds(MinecraftServer server) {
        worldsLoaded = true;
        if (serverStarted && !summoned) {
            FakePlayerManager.getInstance().summonFakePlayers(server);
            summoned = true;
        }
        EntityOptimizationManager.init(server);
        GlobalScoreboardManager.init(server);
        HighlightManager.init(server);
        PlayerEventListener.init(server);
        FakePlayerManager.init(server);
        if (EUNCarpetSettings.craftableInvisibleItemFrames) {
            InvisibleFrameManager.getInstance().registerRecipe(server);
        }
    }

    @Override
    public void onPlayerLoggedIn(ServerPlayer player) {
        PlayerEventListener.getInstance().onPlayerLoggedIn(player);
    }

    @Override
    public void onPlayerLoggedOut(ServerPlayer player) {
        PlayerEventListener.getInstance().onPlayerLoggedOut(player);
    }

    @Override
    public void onTick(MinecraftServer server) {
        FakePlayerManager.getInstance().tick(server);
        EntityOptimizationManager.getInstance().tick(server);
        GlobalScoreboardManager.getInstance().tick();
        PlayerEventListener.getInstance().tick(server);
        HighlightManager.getInstance().tick(server);
        PacketManager.tick(server);

        boolean currentPrefix = EUNCarpetSettings.fakePlayerPrefix;
        if (lastFakePlayerPrefix != currentPrefix) {
            lastFakePlayerPrefix = currentPrefix;
            if (currentPrefix) {
                FakePlayerManager.getInstance().applyPrefixToAllFakePlayers(server);
            } else {
                FakePlayerManager.getInstance().removePrefixFromAllFakePlayers(server);
            }
        }

        boolean currentFrame = EUNCarpetSettings.craftableInvisibleItemFrames;
        if (lastCraftableInvisibleItemFrames != currentFrame) {
            lastCraftableInvisibleItemFrames = currentFrame;
            if (currentFrame) {
                InvisibleFrameManager.getInstance().registerRecipe(server);
            } else {
                InvisibleFrameManager.getInstance().unregisterRecipe(server);
            }
        }

        String currentPearl = EUNCarpetSettings.pearlCannonEnabled;
        String currentHighlight = EUNCarpetSettings.highlightEnabled;
        String currentPacket = EUNCarpetSettings.packetEnabled;
        if (!currentPearl.equals(lastPearlCannonEnabled) ||
                !currentHighlight.equals(lastHighlightEnabled) ||
                !currentPacket.equals(lastPacketEnabled)) {
            lastPearlCannonEnabled = currentPearl;
            lastHighlightEnabled = currentHighlight;
            lastPacketEnabled = currentPacket;
            refreshCommands(server);
        }
    }

    @Override
    public void onServerClosed(MinecraftServer server) {
        FakePlayerManager.getInstance().onServerClosed(server);
        PlayerEventListener.getInstance().onServerClosed();
        HighlightManager.getInstance().onServerClosed();
        PacketManager.save();
    }

    @Override
    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext) {
        this.cachedDispatcher = dispatcher;
        this.cachedBuildContext = commandBuildContext;
        EUNCommands.register(dispatcher, commandBuildContext);
    }

    private void refreshCommands(MinecraftServer server) {
        if (cachedDispatcher == null || cachedBuildContext == null) return;
        cachedDispatcher.getRoot().getChildren().removeIf(node -> node.getName().equals("eun"));
        EUNCommands.register(cachedDispatcher, cachedBuildContext);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            server.getCommands().sendCommands(player);
        }
    }

    @Override
    public carpet.api.settings.SettingsManager extensionSettingsManager() {
        return null;
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public void registerLoggers() {}

    @Override
    public Map<String, String> canHasTranslations(String lang) {
        if ("zh_cn".equals(lang)) {
            return loadTranslationsFromJson("/assets/eun_carpet/lang/zh_cn.json");
        } else if ("en_us".equals(lang)) {
            return loadTranslationsFromJson("/assets/eun_carpet/lang/en_us.json");
        }
        return Collections.emptyMap();
    }

    private Map<String, String> loadTranslationsFromJson(String resourcePath) {
        try (InputStream inputStream = EUNCarpetExtension.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                LOGGER.warn("Translation file not found: {}", resourcePath);
                return Collections.emptyMap();
            }
            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            return gson.fromJson(json, type);
        } catch (Exception e) {
            LOGGER.error("Failed to load translations from {}", resourcePath, e);
            return Collections.emptyMap();
        }
    }

    private Map<String, String> loadTranslationsFromJson() {
        try (InputStream inputStream = EUNCarpetExtension.class.getResourceAsStream("/assets/eun_carpet/lang/zh_cn.json")) {
            if (inputStream == null) {
                LOGGER.warn("Translation file zh_cn.json not found");
                return Collections.emptyMap();
            }
            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            return gson.fromJson(json, type);
        } catch (Exception e) {
            LOGGER.error("Failed to load translations", e);
            return Collections.emptyMap();
        }
    }

    @Override
    public void scarpetApi(carpet.script.CarpetExpression expression) {}
}
