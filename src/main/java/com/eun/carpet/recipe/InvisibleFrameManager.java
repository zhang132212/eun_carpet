package com.eun.carpet.recipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class InvisibleFrameManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("EUNCarpet|InvisibleFrame");
    public static final Identifier RECIPE_ID = Identifier.fromNamespaceAndPath("eun_carpet", "invisible_item_frame");
    private static InvisibleFrameManager instance;
    private boolean registered = false;

    private InvisibleFrameManager() {}

    public static InvisibleFrameManager getInstance() {
        if (instance == null) {
            instance = new InvisibleFrameManager();
        }
        return instance;
    }

    public void registerRecipe(MinecraftServer server) {
        net.minecraft.world.item.crafting.RecipeManager recipeManager = server.getRecipeManager();
        ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(Registries.RECIPE, RECIPE_ID);

        if (recipeManager.byKey(recipeKey).isPresent()) {
            registered = true;
            return;
        }

        ShapedRecipe recipe = createRecipe();
        RecipeHolder<?> holder = new RecipeHolder<>(recipeKey, recipe);

        if (addRecipeToManager(recipeManager, holder)) {
            registered = true;
            LOGGER.info("Registered invisible item frame recipe");
        } else {
            LOGGER.error("Failed to register invisible item frame recipe");
        }
    }

    public void unregisterRecipe(MinecraftServer server) {
        net.minecraft.world.item.crafting.RecipeManager recipeManager = server.getRecipeManager();
        ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(Registries.RECIPE, RECIPE_ID);

        if (removeRecipeFromManager(recipeManager, recipeKey)) {
            registered = false;
            LOGGER.info("Unregistered invisible item frame recipe");
            stopAllCrafters(server);
        } else {
            LOGGER.error("Failed to unregister invisible item frame recipe");
        }
    }

    @SuppressWarnings("unchecked")
    private void stopAllCrafters(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            try {
                ServerChunkCache chunkCache = level.getChunkSource();
                Field chunkMapField = ServerChunkCache.class.getDeclaredField("chunkMap");
                chunkMapField.setAccessible(true);
                Object chunkMap = chunkMapField.get(chunkCache);

                Field visibleChunkMapField = null;
                String[] possibleNames = {"visibleChunkMap", "visibleMap", "chunkHolders", "updatingChunkHolders", "chunks"};
                for (String name : possibleNames) {
                    try {
                        visibleChunkMapField = chunkMap.getClass().getDeclaredField(name);
                        break;
                    } catch (NoSuchFieldException ignored) {}
                }
                if (visibleChunkMapField == null) {
                    LOGGER.error("Could not find visible chunk map field in ChunkMap, cannot stop crafters");
                    return;
                }
                visibleChunkMapField.setAccessible(true);
                Map<Long, ChunkHolder> visibleChunkMap = (Map<Long, ChunkHolder>) visibleChunkMapField.get(chunkMap);

                for (ChunkHolder holder : visibleChunkMap.values()) {
                    LevelChunk chunk = holder.getTickingChunk();
                    if (chunk == null) {
                        ChunkResult<LevelChunk> result = holder.getFullChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK);
                        chunk = result.orElse(null);
                    }
                    if (chunk != null) {
                        for (BlockEntity be : chunk.getBlockEntities().values()) {
                            if (be instanceof CrafterBlockEntity crafter) {
                                stopCrafterIfCrafting(crafter, level);
                            }
                        }
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                LOGGER.error("Failed to access chunk map, cannot stop crafters", e);
            }
        }
    }

    private void stopCrafterIfCrafting(CrafterBlockEntity crafter, ServerLevel level) {
        try {
            Field craftingTicksField = CrafterBlockEntity.class.getDeclaredField("craftingTicksRemaining");
            craftingTicksField.setAccessible(true);
            int remaining = (int) craftingTicksField.get(crafter);
            if (remaining > 0) {
                crafter.setCraftingTicksRemaining(0);
                BlockPos pos = crafter.getBlockPos();
                BlockState state = crafter.getBlockState();
                if (state.hasProperty(CrafterBlock.CRAFTING)) {
                    level.setBlock(pos, state.setValue(CrafterBlock.CRAFTING, false), 3);
                }
                crafter.setTriggered(false);
                level.sendBlockUpdated(pos, state, state, 3);
                LOGGER.debug("Stopped crafting in crafter at {}", pos);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.error("Failed to access craftingTicksRemaining field in crafter at {}", crafter.getBlockPos(), e);
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

    private ShapedRecipe createRecipe() {
        Map<Character, Ingredient> key = Map.of(
                'a', Ingredient.of(Items.ITEM_FRAME),
                'b', Ingredient.of(Items.AMETHYST_SHARD)
        );

        ShapedRecipePattern pattern = ShapedRecipePattern.of(key, "aaa", "aba", "aaa");

        ItemStack result = new ItemStack(Items.ITEM_FRAME, 8);

        // 隐形组件
        CompoundTag entityTag = new CompoundTag();
        entityTag.putBoolean("Invisible", true);
        TypedEntityData<EntityType<?>> data = TypedEntityData.of(EntityType.ITEM_FRAME, entityTag);
        result.set(DataComponents.ENTITY_DATA, data);

        // 附魔光泽
        result.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        // 自定义名称
        result.set(DataComponents.CUSTOM_NAME, Component.literal("隐形展示框"));

        return new ShapedRecipe(
                "",
                CraftingBookCategory.MISC,
                pattern,
                result,
                true
        );
    }

    public boolean isRegistered() {
        return registered;
    }
}