package com.eun.carpet.client;

import com.eun.carpet.client.config.ModConfig;
import com.eun.carpet.client.feature.ClientHighlightManager;
import com.eun.carpet.client.feature.ScoreboardHider;
import com.eun.carpet.client.network.ClientHideEntitiesPayloadHandler;
import com.eun.carpet.client.optimization.ClientTransparencyManager;
import com.eun.carpet.highlight.HighlightPayload;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class EUNCarpetClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        ClientHideEntitiesPayloadHandler.register();
        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> ClientTransparencyManager.getInstance().onEntityAdded(entity));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientTransparencyManager.getInstance().clear();
            ClientHighlightManager.INSTANCE.update(false, false, 0xFFFFFFFF, 0xFFFFFFFF);
        });

        ScoreboardHider.init();
        ClientTickEvents.END_CLIENT_TICK.register(client -> ScoreboardHider.tick());

        // 在连接服务器时注册网络接收器
        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            ClientPlayNetworking.registerReceiver(HighlightPayload.TYPE, (payload, context) -> {
                context.client().execute(() -> {
                    ClientHighlightManager.INSTANCE.update(
                            payload.highlightItems(),
                            payload.highlightEntities(),
                            payload.itemColor(),
                            payload.entityColor()
                    );
                });
            });
        });
    }
}