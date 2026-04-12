package com.eun.carpet.client.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

public class ScoreboardHider {
    private static final Minecraft client = Minecraft.getInstance();
    private static boolean hidden = false; // true 表示隐藏
    private static long lastToggleTime = 0;
    private static final long TOGGLE_COOLDOWN_MS = 10000; // 10秒

    public static void init() {
    }

    public static void tick() {
        if (client.player == null) return;

        // 俯仰角 ≤ -85°+ 潜行单击
        float pitch = client.player.getXRot();
        boolean lookingUp = pitch <= -85.0F;
        boolean sneakPressed = client.options.keyShift.consumeClick();

        if (lookingUp && sneakPressed) {
            long now = System.currentTimeMillis();
            if (now - lastToggleTime > TOGGLE_COOLDOWN_MS) {
                hidden = !hidden;
                lastToggleTime = now;
                client.player.displayClientMessage(
                        Component.literal("计分板已" + (hidden ? "§c隐藏" : "§a显示") + "（10秒冷却）"), false);
            } else {
                long remaining = (TOGGLE_COOLDOWN_MS - (now - lastToggleTime)) / 1000;
                client.player.displayClientMessage(
                        Component.literal("§c冷却中，剩余 " + remaining + " 秒"), true);
            }
        }
    }

    public static boolean isHidden() {
        return hidden;
    }
}