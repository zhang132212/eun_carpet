package com.eun.carpet.recipe;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.core.registries.Registries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 运行时注册/注销玩家头颅合成配方(不依赖数据包)。
 * 两个配方: player_head(1个头) / player_head_x8(8个头)。
 */
public class PlayerHeadManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("EUNCarpet|PlayerHead");
    public static final Identifier RECIPE_ID = Identifier.fromNamespaceAndPath("eun_carpet", "player_head");
    public static final Identifier RECIPE_ID_X8 = Identifier.fromNamespaceAndPath("eun_carpet", "player_head_x8");

    private static PlayerHeadManager instance;
    private boolean registered = false;

    private PlayerHeadManager() {}

    public static PlayerHeadManager getInstance() {
        if (instance == null) {
            instance = new PlayerHeadManager();
        }
        return instance;
    }

    public void registerRecipe(MinecraftServer server) {
        RecipeManager recipeManager = server.getRecipeManager();
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, RECIPE_ID);
        ResourceKey<Recipe<?>> key8 = ResourceKey.create(Registries.RECIPE, RECIPE_ID_X8);

        if (recipeManager.byKey(key).isPresent() && recipeManager.byKey(key8).isPresent()) {
            registered = true;
            return;
        }

        boolean ok1 = addRecipeToManager(recipeManager, new RecipeHolder<>(key, new PlayerHeadRecipe(1)));
        boolean ok2 = addRecipeToManager(recipeManager, new RecipeHolder<>(key8, new PlayerHeadRecipe(8)));
        if (ok1 && ok2) {
            registered = true;
            LOGGER.info("Registered player head recipes (x1, x8)");
        } else {
            LOGGER.error("Failed to register player head recipes");
        }
    }

    public void unregisterRecipe(MinecraftServer server) {
        RecipeManager recipeManager = server.getRecipeManager();
        boolean ok1 = removeRecipeFromManager(recipeManager, ResourceKey.create(Registries.RECIPE, RECIPE_ID));
        boolean ok2 = removeRecipeFromManager(recipeManager, ResourceKey.create(Registries.RECIPE, RECIPE_ID_X8));
        if (ok1 && ok2) {
            registered = false;
            LOGGER.info("Unregistered player head recipes");
        } else {
            LOGGER.warn("Failed to fully unregister player head recipes");
        }
    }

    @SuppressWarnings("unchecked")
    private boolean addRecipeToManager(RecipeManager manager, RecipeHolder<?> newHolder) {
        try {
            Field recipesField = findRecipesField(manager);
            RecipeMap recipeMap = (RecipeMap) recipesField.get(manager);

            Collection<RecipeHolder<?>> existing = recipeMap.values();
            List<RecipeHolder<?>> newList = new ArrayList<>(existing);
            newList.add(newHolder);

            RecipeMap newMap = RecipeMap.create(newList);
            recipesField.set(manager, newMap);
            return true;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.error("Failed to add recipe", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean removeRecipeFromManager(RecipeManager manager, ResourceKey<Recipe<?>> keyToRemove) {
        try {
            Field recipesField = findRecipesField(manager);
            RecipeMap recipeMap = (RecipeMap) recipesField.get(manager);

            Collection<RecipeHolder<?>> existing = recipeMap.values();
            List<RecipeHolder<?>> newList = new ArrayList<>();
            for (RecipeHolder<?> holder : existing) {
                if (!holder.id().equals(keyToRemove)) {
                    newList.add(holder);
                }
            }

            if (newList.size() == existing.size()) {
                return false;
            }

            RecipeMap newMap = RecipeMap.create(newList);
            recipesField.set(manager, newMap);
            return true;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.error("Failed to remove recipe", e);
            return false;
        }
    }

    private Field findRecipesField(RecipeManager manager) throws NoSuchFieldException {
        for (Field field : RecipeManager.class.getDeclaredFields()) {
            if (field.getType() == RecipeMap.class) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new NoSuchFieldException("Could not find recipes field in RecipeManager");
    }

    public boolean isRegistered() {
        return registered;
    }
}
