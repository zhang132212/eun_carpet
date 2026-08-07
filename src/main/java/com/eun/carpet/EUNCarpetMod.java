package com.eun.carpet;

import carpet.CarpetServer;
import com.eun.carpet.commandgui.ServerCommandConfigManager;
import com.eun.carpet.commandgui.ServerCommandPayload;
import com.eun.carpet.config.EUNConfigManager;
import com.eun.carpet.scoreboard.GlobalScoreboardPayload;
import com.eun.carpet.optimization.HideEntitiesPayload;
import com.eun.carpet.highlight.HighlightPayload;
import com.eun.carpet.pearlcannon.PearlCannonManager;
import com.eun.carpet.recipe.InvisibleFrameManager;
import com.eun.carpet.recipe.PlayerHeadManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

public class EUNCarpetMod implements ModInitializer {
    public static final String MOD_ID = "eun_carpet";

    /** 当前服务器实例 (合成配方等需要) */
    public static MinecraftServer server;

    @Override
    public void onInitialize() {
        CarpetServer.manageExtension(new EUNCarpetExtension());

        PayloadTypeRegistry.clientboundPlay().register(HideEntitiesPayload.TYPE, HideEntitiesPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(GlobalScoreboardPayload.TYPE, GlobalScoreboardPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HighlightPayload.TYPE, HighlightPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ServerCommandPayload.TYPE, ServerCommandPayload.CODEC);

        //初始化珍珠炮管理器，确保配置目录和默认文件存在
        PearlCannonManager.getInstance();

        // 服务器启动后加载 command-gui 指令同步配置
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            EUNCarpetMod.server = server;
            ServerCommandConfigManager.load(EUNConfigManager.getConfigRoot());
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            EUNCarpetMod.server = null;
        });

        //数据包重载后重新注册配方
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResourceManager, success) -> {
            if (!success) return;
            if (EUNCarpetSettings.craftableInvisibleItemFrames) {
                InvisibleFrameManager.getInstance().registerRecipe(server);
            }
            if (EUNCarpetSettings.craftablePlayerHeads) {
                PlayerHeadManager.getInstance().registerRecipe(server);
            }
        });
    }
}
