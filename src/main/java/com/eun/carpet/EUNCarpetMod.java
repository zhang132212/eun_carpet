package com.eun.carpet;

import carpet.CarpetServer;
import com.eun.carpet.scoreboard.GlobalScoreboardPayload;
import com.eun.carpet.optimization.HideEntitiesPayload;
import com.eun.carpet.highlight.HighlightPayload;
import com.eun.carpet.pearlcannon.PearlCannonManager;
import com.eun.carpet.recipe.InvisibleFrameManager; // ========== 新增 ==========
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents; // ========== 新增 ==========

public class EUNCarpetMod implements ModInitializer {
    public static final String MOD_ID = "eun_carpet";

    @Override
    public void onInitialize() {
        CarpetServer.manageExtension(new EUNCarpetExtension());

        PayloadTypeRegistry.playS2C().register(HideEntitiesPayload.TYPE, HideEntitiesPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GlobalScoreboardPayload.TYPE, GlobalScoreboardPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HighlightPayload.TYPE, HighlightPayload.CODEC);

        //初始化珍珠炮管理器，确保配置目录和默认文件存在
        PearlCannonManager.getInstance();

        //数据包重载后重新注册配方
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResourceManager, success) -> {
            if (success && EUNCarpetSettings.craftableInvisibleItemFrames) {
                InvisibleFrameManager.getInstance().registerRecipe(server);
            }
        });
    }
}