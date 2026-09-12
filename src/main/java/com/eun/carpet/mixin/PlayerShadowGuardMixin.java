package com.eun.carpet.mixin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.context.CommandContext;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 关掉 Carpet 的 /player <id> shadow 动作（VC 端可利用它刷物品）。
 * 目标：carpet/commands/PlayerCommand 的 shadow(CommandContext) 方法（Carpet 本体，非映射名 → remap=false）。
 * 配置：config/eun_carpet/player-shadow.json  {"disable": true}；默认 disable=true（谁都用不了，含 OP）。
 */
@Mixin(targets = "carpet.commands.PlayerCommand", remap = false)
public class PlayerShadowGuardMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("eun_carpet");
    private static Boolean disabled;

    @Inject(method = "shadow", at = @At("HEAD"), cancellable = true, remap = false)
    private static void eun$blockShadow(CommandContext<CommandSourceStack> context, CallbackInfoReturnable<Integer> cir) {
        if (!isDisabled()) {
            return;
        }
        CommandSourceStack source = context.getSource();
        LOGGER.warn("[eun_carpet] 拦截 /player shadow：执行者={} 位置={}",
                source.getTextName(), source.getPosition());
        source.sendFailure(Component.literal("本服已禁用 /player shadow"));
        cir.setReturnValue(0);
    }

    private static boolean isDisabled() {
        if (disabled != null) {
            return disabled;
        }
        boolean value = true;
        try {
            Path file = FabricLoader.getInstance().getConfigDir().resolve("eun_carpet").resolve("player-shadow.json");
            if (Files.isRegularFile(file)) {
                JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                if (root.has("disable")) {
                    value = root.get("disable").getAsBoolean();
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[eun_carpet] player-shadow.json 读取失败，按默认禁用处理", e);
        }
        disabled = value;
        LOGGER.info("[eun_carpet] /player shadow 拦截开关：{}", value ? "已禁用" : "放行");
        return value;
    }
}
