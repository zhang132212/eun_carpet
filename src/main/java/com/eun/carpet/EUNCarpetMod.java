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
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class EUNCarpetMod implements ModInitializer {
    public static final String MOD_ID = "eun_carpet";

    @Override
    public void onInitialize() {
        CarpetServer.manageExtension(new EUNCarpetExtension());

        PayloadTypeRegistry.playS2C().register(HideEntitiesPayload.TYPE, HideEntitiesPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GlobalScoreboardPayload.TYPE, GlobalScoreboardPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HighlightPayload.TYPE, HighlightPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerCommandPayload.TYPE, ServerCommandPayload.CODEC);

        //初始化珍珠炮管理器，确保配置目录和默认文件存在
        PearlCannonManager.getInstance();

        // 服务器启动后加载 command-gui 指令同步配置
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                ServerCommandConfigManager.load(EUNConfigManager.getConfigRoot()));

        //数据包重载后重新注册配方
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResourceManager, success) -> {
            if (success && EUNCarpetSettings.craftableInvisibleItemFrames) {
                InvisibleFrameManager.getInstance().registerRecipe(server);
            }
        });
    }
}