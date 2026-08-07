package com.eun.carpet.mixin;

import com.eun.carpet.recipe.InvisibleFrameManager;
import com.eun.carpet.recipe.PlayerHeadManager;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("EUNCarpet|Mixin");

    @Inject(
            method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;",
            at = @At("RETURN"),
            cancellable = true
    )
    private <I extends RecipeInput, T extends Recipe<I>> void onGetRecipeFor(
            RecipeType<T> recipeType,
            I input,
            Level level,
            CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir
    ) {
        Optional<RecipeHolder<T>> optional = cir.getReturnValue();
        if (optional.isPresent()) {
            RecipeHolder<T> holder = optional.get();
            // 使用 identifier() 而不是 location()
            if (holder.id().identifier().equals(InvisibleFrameManager.RECIPE_ID)) {
                if (input instanceof CrafterBlockEntity) {
                    LOGGER.info("Blocked invisible frame recipe for crafter at {}", ((CrafterBlockEntity) input).getBlockPos());
                    cir.setReturnValue(Optional.empty());
                }
            }
            // 玩家头颅配方禁止合成器自动合成(命名牌+头 会被高频触发网络解析, 卡服风险)
            if (holder.id().identifier().equals(PlayerHeadManager.RECIPE_ID)
                    || holder.id().identifier().equals(PlayerHeadManager.RECIPE_ID_X8)) {
                if (input instanceof CrafterBlockEntity) {
                    LOGGER.info("Blocked player head recipe for crafter at {}", ((CrafterBlockEntity) input).getBlockPos());
                    cir.setReturnValue(Optional.empty());
                }
            }
        }
    }
}