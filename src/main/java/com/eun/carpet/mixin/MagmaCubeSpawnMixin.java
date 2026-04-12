package com.eun.carpet.mixin;

import com.eun.carpet.EUNCarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MagmaCube.class)
public abstract class MagmaCubeSpawnMixin {

    @Inject(
            method = "checkMagmaCubeSpawnRules",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void wuhu$suppressInNetherWastes(
            EntityType<MagmaCube> entityType,
            LevelAccessor levelAccessor,
            EntitySpawnReason entitySpawnReason,
            BlockPos blockPos,
            RandomSource randomSource,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!EUNCarpetSettings.suppressMagmaCubeInNetherWastes) {
            return;
        }
        // 使用正确的变量名
        if (entitySpawnReason == EntitySpawnReason.SPAWNER) {
            return;
        }

        // 获取生物群系并检查是否为下界荒地
        Holder<Biome> biomeHolder = levelAccessor.getBiome(blockPos);
        ResourceKey<Biome> biomeKey = biomeHolder.unwrapKey().orElse(null);

        if (Biomes.NETHER_WASTES.equals(biomeKey)) {
            cir.setReturnValue(false);
        }
    }
}