package com.eun.carpet.recipe;

import com.eun.carpet.EUNCarpetMod;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

/**
 * 玩家头颅合成配方:
 * - headCount=1: 1个(已命名)命名牌 + 1个凋零骷髅头 -> 1个头
 * - headCount=8: 1个(已命名)命名牌 + 8个凋零骷髅头 -> 8个头
 * 命名牌名字对应正版玩家 -> 该玩家头颅(含皮肤); 否则普通史蒂夫头颅。
 */
public class PlayerHeadRecipe implements CraftingRecipe {
    private final int headCount;

    public PlayerHeadRecipe(int headCount) {
        this.headCount = headCount;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int nameTags = 0;
        int skulls = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(Items.NAME_TAG) && stack.has(DataComponents.CUSTOM_NAME)) {
                nameTags++;
            } else if (stack.is(Items.WITHER_SKELETON_SKULL)) {
                skulls++;
            } else {
                return false;
            }
        }
        return nameTags == 1 && skulls == headCount;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack result = new ItemStack(Items.PLAYER_HEAD, headCount);
        String name = extractName(input);
        if (name != null) {
            MinecraftServer server = EUNCarpetMod.server;
            GameProfile profile = PlayerHeadResolver.resolveFullSync(name, server);
            if (profile != null) {
                result.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
            }
        }
        return result;
    }

    private static String extractName(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.is(Items.NAME_TAG)) {
                Component name = stack.get(DataComponents.CUSTOM_NAME);
                if (name != null) {
                    String s = name.getString();
                    if (s != null && !s.isEmpty()) return s.trim();
                }
            }
        }
        return null;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return ShapelessRecipe.SERIALIZER;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return new RecipeBookCategory();
    }
}
