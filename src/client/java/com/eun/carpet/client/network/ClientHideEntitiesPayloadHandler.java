package com.eun.carpet.client.network;

import com.eun.carpet.client.optimization.ClientTransparencyManager;
import com.eun.carpet.optimization.HideEntitiesPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public class ClientHideEntitiesPayloadHandler {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(HideEntitiesPayload.TYPE, (payload, context) -> context.client().execute(() -> {
            Minecraft mc = Minecraft.getInstance();

            // 更新透明管理器
            ClientTransparencyManager.getInstance().update(payload.added(), payload.removed());

            // 处理尚未加载的实体
            for (int id : payload.added()) {
                if (mc.level == null) continue;
                if (mc.level.getEntity(id) == null) {
                    ClientTransparencyManager.getInstance().markPending(id);
                }
            }
        }));
    }
}