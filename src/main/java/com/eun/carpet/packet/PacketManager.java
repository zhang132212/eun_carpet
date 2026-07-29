package com.eun.carpet.packet;

import carpet.patches.EntityPlayerMPFake;
import com.eun.carpet.EUNCarpetMod;
import com.eun.carpet.config.EUNConfigManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class PacketManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EUNCarpetMod.MOD_ID + "|Packet");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = EUNConfigManager.getConfigRoot().resolve("packet");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("configs.json");

    private static final Map<UUID, PacketConfig> CONFIGS = new HashMap<>();
    private static final Set<UUID> PACKING_PLAYERS = new HashSet<>();
    private static final Map<UUID, Long> FAIL_COOLDOWN = new HashMap<>();
    private static final long COOLDOWN_MILLIS = 1000;

    private static final int MAIN_SIZE = 36;
    private static final int OFF_HAND_SLOT = 40;
    private static final int MAX_ITEMS_PER_BOX = 27 * 64;
    private static final int PACKET_INTERVAL = 20;

    static {
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            LOGGER.error("无法创建配置目录", e);
        }
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> save());
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH)) {
            Map<String, PacketConfig> raw = GSON.fromJson(reader, new TypeToken<Map<String, PacketConfig>>(){}.getType());
            if (raw != null) {
                CONFIGS.clear();
                raw.forEach((uuidStr, config) -> {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        CONFIGS.put(uuid, config);
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("无效的UUID: {}", uuidStr);
                    }
                });
            }
        } catch (IOException e) {
            LOGGER.error("加载打包配置失败", e);
        }
    }

    public static void save() {
        Map<String, PacketConfig> toSave = new HashMap<>();
        CONFIGS.forEach((uuid, config) -> toSave.put(uuid.toString(), config));
        try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(toSave, writer);
        } catch (IOException e) {
            LOGGER.error("保存打包配置失败", e);
        }
    }

    public static PacketConfig getConfig(UUID uuid) {
        return CONFIGS.get(uuid);
    }

    public static void setConfig(UUID uuid, PacketConfig config) {
        CONFIGS.put(uuid, config);
        save();
    }

    public static void removeConfig(UUID uuid) {
        CONFIGS.remove(uuid);
        save();
    }

    public static Map<UUID, PacketConfig> getAllConfigs() {
        return Collections.unmodifiableMap(CONFIGS);
    }

    public static void tick(MinecraftServer server) {
        int currentTick = server.getTickCount();
        for (EntityPlayerMPFake fakePlayer : getFakePlayers(server)) {
            UUID uuid = fakePlayer.getUUID();
            PacketConfig config = CONFIGS.get(uuid);
            if (config == null || !config.isEnabled()) continue;

            if (currentTick - config.getLastTick() < PACKET_INTERVAL) continue;
            if (PACKING_PLAYERS.contains(uuid)) continue;

            PackResult result = tryPack(fakePlayer, config.getType());
            config.setLastTick(currentTick);
            PackResult previous = config.getLastResult();
            config.setLastResult(result);

            if (previous != result) {
                broadcastStateChange(server, fakePlayer.getName().getString(), result);
            }
        }
    }

    private static List<EntityPlayerMPFake> getFakePlayers(MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream()
                .filter(p -> p instanceof EntityPlayerMPFake)
                .map(p -> (EntityPlayerMPFake) p)
                .collect(Collectors.toList());
    }

    private static PackResult tryPack(EntityPlayerMPFake fakePlayer, int type) {
        UUID uuid = fakePlayer.getUUID();
        if (PACKING_PLAYERS.contains(uuid)) return PackResult.INTERNAL_ERROR;
        PACKING_PLAYERS.add(uuid);
        try {
            return doPack(fakePlayer, type);
        } catch (Exception e) {
            LOGGER.error("假人 {} 打包异常", fakePlayer.getName().getString(), e);
            return PackResult.INTERNAL_ERROR;
        } finally {
            PACKING_PLAYERS.remove(uuid);
        }
    }

    private static PackResult doPack(EntityPlayerMPFake fakePlayer, int type) {
        Inventory inv = fakePlayer.getInventory();

        // 检查副手工作台
        if (!inv.getItem(OFF_HAND_SLOT).is(Items.CRAFTING_TABLE)) {
            return PackResult.NO_CRAFTING_TABLE;
        }

        int freeSlots = countFreeSlots(inv);
        if (freeSlots > type + 2) {
            return PackResult.SUCCESS;
        }

        List<Integer> logBoxSlots = new ArrayList<>();
        List<Integer> shellBoxSlots = new ArrayList<>();
        Item requiredLogType = null;

        for (int i = 0; i < MAIN_SIZE; i++) {
            ItemStack stack = inv.getItem(i);
            if (isShulkerBox(stack)) {
                Item logType = getPureLogType(stack);
                if (logType != null) {
                    if (requiredLogType == null) {
                        requiredLogType = logType;
                    } else if (logType != requiredLogType) {
                        return PackResult.MIXED_LOG_TYPES;
                    }
                    logBoxSlots.add(i);
                }
                if (isPureShellBox(stack)) {
                    shellBoxSlots.add(i);
                }
            }
        }

        if (logBoxSlots.isEmpty()) return PackResult.NO_LOG_BOX;
        if (shellBoxSlots.isEmpty()) return PackResult.NO_SHELL_BOX;

        long totalLogs = sumItemsInBoxes(inv, logBoxSlots);
        long totalShells = sumItemsInBoxes(inv, shellBoxSlots);
        long neededLogs = 2L * type;
        long neededShells = 2L * type;
        if (totalLogs < neededLogs || totalShells < neededShells) {
            return PackResult.INSUFFICIENT_MATERIALS;
        }

        Map<Item, Long> stackableItems = collectStackableItems(inv);
        if (stackableItems.isEmpty()) {
            return PackResult.SUCCESS;
        }

        List<Map.Entry<Item, Long>> sorted = stackableItems.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(type)
                .collect(Collectors.toList());
        int actualTypes = sorted.size(); // 实际可打包种类数（可能小于type）

        int neededFreeSlots = 2 + actualTypes;
        if (freeSlots < neededFreeSlots) {
            boolean spaceFreed = makeSpace(fakePlayer, neededFreeSlots - freeSlots);
            if (!spaceFreed) {
                return PackResult.INSUFFICIENT_SPACE;
            }
            freeSlots = countFreeSlots(inv);
            if (freeSlots < neededFreeSlots) {
                return PackResult.INSUFFICIENT_SPACE;
            }
        }

        List<Integer> freeIndices = getFreeSlotIndices(inv);
        int materialLogSlot = freeIndices.get(0);
        int materialShellSlot = freeIndices.get(1);
        List<Integer> boxSlots = freeIndices.subList(2, 2 + actualTypes);

        if (!extractFromBoxes(inv, logBoxSlots, materialLogSlot, neededLogs, requiredLogType)) {
            return PackResult.INTERNAL_ERROR;
        }
        if (!extractFromBoxes(inv, shellBoxSlots, materialShellSlot, neededShells, Items.SHULKER_SHELL)) {
            return PackResult.INTERNAL_ERROR;
        }

        ItemStack logStack = inv.getItem(materialLogSlot);
        logStack.shrink((int) neededLogs);
        if (logStack.isEmpty()) inv.setItem(materialLogSlot, ItemStack.EMPTY);
        ItemStack shellStack = inv.getItem(materialShellSlot);
        shellStack.shrink((int) neededShells);
        if (shellStack.isEmpty()) inv.setItem(materialShellSlot, ItemStack.EMPTY);

        for (int slot : boxSlots) {
            inv.setItem(slot, new ItemStack(Items.SHULKER_BOX));
        }

        for (int i = 0; i < actualTypes; i++) {
            Map.Entry<Item, Long> entry = sorted.get(i);
            Item itemType = entry.getKey();
            long totalCount = entry.getValue();
            long putCount = Math.min(totalCount, MAX_ITEMS_PER_BOX);

            ItemStack boxStack = inv.getItem(boxSlots.get(i));
            if (!boxStack.is(Items.SHULKER_BOX)) continue;

            List<ItemStack> contents = new ArrayList<>();
            long remaining = putCount;
            while (remaining > 0) {
                int stackSize = (int) Math.min(remaining, 64);
                contents.add(new ItemStack(itemType, stackSize));
                remaining -= stackSize;
            }
            ItemContainerContents container = ItemContainerContents.fromItems(contents);
            boxStack.set(DataComponents.CONTAINER, container);

            removeItemsFromInventory(inv, itemType, putCount);
        }

        discardNonStackableNonBoxes(fakePlayer);

        for (int boxSlot : boxSlots) {
            ItemStack box = inv.getItem(boxSlot);
            if (!box.isEmpty()) {
                fakePlayer.drop(box, false, true);
                inv.setItem(boxSlot, ItemStack.EMPTY);
            }
        }

        for (int i = 0; i < MAIN_SIZE; i++) {
            if (logBoxSlots.contains(i) || shellBoxSlots.contains(i)) continue;
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !isShulkerBox(stack)) continue;
            fakePlayer.drop(stack.copy(), false, true);
            inv.setItem(i, ItemStack.EMPTY);
        }

        return PackResult.SUCCESS;
    }

    // --------------------- 辅助方法（与之前相同，此处省略，确保无 k 变量残留）---------------------
    private static int countFreeSlots(Inventory inv) {
        int count = 0;
        for (int i = 0; i < MAIN_SIZE; i++) {
            if (inv.getItem(i).isEmpty()) count++;
        }
        return count;
    }

    private static List<Integer> getFreeSlotIndices(Inventory inv) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < MAIN_SIZE; i++) {
            if (inv.getItem(i).isEmpty()) list.add(i);
        }
        return list;
    }

    private static boolean isShulkerBox(ItemStack stack) {
        return stack.is(ItemTags.SHULKER_BOXES);
    }

    private static Item getPureLogType(ItemStack box) {
        if (!isShulkerBox(box)) return null;
        ItemContainerContents container = box.get(DataComponents.CONTAINER);
        if (container == null) return null;
        Item logType = null;
        for (ItemStack inner : container.allItemsCopyStream().toList()) {
            if (inner.isEmpty()) continue;
            if (!isLog(inner)) return null;
            if (logType == null) {
                logType = inner.getItem();
            } else if (logType != inner.getItem()) {
                return null;
            }
        }
        return logType;
    }

    private static boolean isLog(ItemStack stack) {
        return stack.is(ItemTags.LOGS);
    }

    private static boolean isPureShellBox(ItemStack box) {
        if (!isShulkerBox(box)) return false;
        ItemContainerContents container = box.get(DataComponents.CONTAINER);
        if (container == null) return false;
        for (ItemStack inner : container.allItemsCopyStream().toList()) {
            if (inner.isEmpty()) continue;
            if (!inner.is(Items.SHULKER_SHELL)) return false;
        }
        return true;
    }

    private static long sumItemsInBoxes(Inventory inv, List<Integer> boxSlots) {
        long sum = 0;
        for (int slot : boxSlots) {
            ItemStack box = inv.getItem(slot);
            if (!isShulkerBox(box)) continue;
            ItemContainerContents container = box.get(DataComponents.CONTAINER);
            if (container != null) {
                for (ItemStack inner : container.allItemsCopyStream().toList()) {
                    if (!inner.isEmpty()) sum += inner.getCount();
                }
            }
        }
        return sum;
    }

    private static boolean extractFromBoxes(Inventory inv, List<Integer> boxSlots, int targetSlot, long needed, Object itemType) {
        Item item = (Item) itemType;
        ItemStack targetStack = inv.getItem(targetSlot);
        if (targetStack.isEmpty()) {
            targetStack = new ItemStack(item, 0);
            inv.setItem(targetSlot, targetStack);
        } else if (!targetStack.is(item)) {
            return false;
        }

        long remaining = needed;
        for (int slot : boxSlots) {
            ItemStack box = inv.getItem(slot);
            if (!isShulkerBox(box)) continue;
            ItemContainerContents container = box.get(DataComponents.CONTAINER);
            if (container == null) continue;

            List<ItemStack> contents = container.allItemsCopyStream().collect(Collectors.toList());
            boolean boxChanged = false;
            for (int idx = 0; idx < contents.size() && remaining > 0; idx++) {
                ItemStack inner = contents.get(idx);
                if (inner.isEmpty()) continue;
                if (!inner.is(item)) continue;
                int take = (int) Math.min(inner.getCount(), remaining);
                if (take > 0) {
                    inner.shrink(take);
                    targetStack.grow(take);
                    remaining -= take;
                    boxChanged = true;
                    if (inner.isEmpty()) {
                        contents.set(idx, ItemStack.EMPTY);
                    }
                }
            }
            if (boxChanged) {
                contents.removeIf(ItemStack::isEmpty);
                ItemContainerContents newContainer = ItemContainerContents.fromItems(contents);
                box.set(DataComponents.CONTAINER, newContainer);
                if (contents.isEmpty()) {
                    inv.setItem(slot, ItemStack.EMPTY);
                }
            }
            if (remaining == 0) break;
        }
        return remaining == 0;
    }

    private static void removeItemsFromInventory(Inventory inv, Item itemType, long amount) {
        long toRemove = amount;
        for (int i = 0; i < MAIN_SIZE; i++) {
            if (toRemove <= 0) break;
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !stack.is(itemType)) continue;
            int remove = (int) Math.min(stack.getCount(), toRemove);
            stack.shrink(remove);
            toRemove -= remove;
            if (stack.isEmpty()) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    private static boolean discardNonStackableNonBoxes(EntityPlayerMPFake fakePlayer) {
        Inventory inv = fakePlayer.getInventory();
        boolean discarded = false;
        for (int i = 0; i < MAIN_SIZE; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            if (isShulkerBox(stack)) continue;
            if (stack.getMaxStackSize() > 1) continue;
            ItemEntity itemEntity = new ItemEntity(
                    fakePlayer.level(),
                    fakePlayer.getX(),
                    fakePlayer.getY(),
                    fakePlayer.getZ(),
                    stack.copy()
            );
            itemEntity.setPickUpDelay(0);
            itemEntity.setThrower(fakePlayer);
            itemEntity.setDeltaMovement(Vec3.ZERO);
            fakePlayer.level().addFreshEntity(itemEntity);
            inv.setItem(i, ItemStack.EMPTY);
            discarded = true;
        }
        return discarded;
    }

    private static Map<Item, Long> collectStackableItems(Inventory inv) {
        Map<Item, Long> map = new HashMap<>();
        for (int i = 0; i < MAIN_SIZE; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            if (isShulkerBox(stack)) continue;
            if (stack.getMaxStackSize() <= 1) continue;
            map.merge(stack.getItem(), (long) stack.getCount(), Long::sum);
        }
        return map;
    }

    private static boolean makeSpace(EntityPlayerMPFake fakePlayer, int need) {
        Inventory inv = fakePlayer.getInventory();
        int freed = 0;

        for (int i = 0; i < MAIN_SIZE && freed < need; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            if (isShulkerBox(stack)) continue;
            if (stack.getMaxStackSize() > 1) continue;
            ItemEntity itemEntity = new ItemEntity(
                    fakePlayer.level(),
                    fakePlayer.getX(),
                    fakePlayer.getY(),
                    fakePlayer.getZ(),
                    stack.copy()
            );
            itemEntity.setPickUpDelay(0);
            itemEntity.setThrower(fakePlayer);
            itemEntity.setDeltaMovement(Vec3.ZERO);
            fakePlayer.level().addFreshEntity(itemEntity);
            inv.setItem(i, ItemStack.EMPTY);
            freed++;
        }
        if (freed >= need) return true;

        List<Integer> candidateSlots = new ArrayList<>();
        for (int i = 0; i < MAIN_SIZE; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            if (isShulkerBox(stack)) continue;
            if (isLog(stack)) continue;
            if (stack.is(Items.SHULKER_SHELL)) continue;
            if (stack.getMaxStackSize() <= 1) continue;
            candidateSlots.add(i);
        }
        Collections.shuffle(candidateSlots);
        for (int slot : candidateSlots) {
            if (freed >= need) break;
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty()) {
                ItemEntity itemEntity = new ItemEntity(
                        fakePlayer.level(),
                        fakePlayer.getX(),
                        fakePlayer.getY(),
                        fakePlayer.getZ(),
                        stack.copy()
                );
                itemEntity.setPickUpDelay(0);
                itemEntity.setThrower(fakePlayer);
                itemEntity.setDeltaMovement(Vec3.ZERO);
                fakePlayer.level().addFreshEntity(itemEntity);
                inv.setItem(slot, ItemStack.EMPTY);
                freed++;
            }
        }
        return freed >= need;
    }

    private static void broadcastStateChange(MinecraftServer server, String playerName, PackResult result) {
        if (server == null) return;
        String message = switch (result) {
            case SUCCESS -> "§b[假人打包] " + playerName + " 打包功能无异常";
            case NO_CRAFTING_TABLE -> "§b[假人打包] " + playerName + " 副手缺少工作台，暂停打包";
            case NO_LOG_BOX -> "§b[假人打包] " + playerName + " 缺少纯原木盒，暂停打包";
            case NO_SHELL_BOX -> "§b[假人打包] " + playerName + " 缺少纯潜影壳盒，暂停打包";
            case MIXED_LOG_TYPES -> "§b[假人打包] " + playerName + " 原木盒种类不统一，暂停打包";
            case INSUFFICIENT_MATERIALS -> "§b[假人打包] " + playerName + " 材料不足，暂停打包";
            case INSUFFICIENT_SPACE -> "§b[假人打包] " + playerName + " 背包空间不足，暂停打包";
            case INTERNAL_ERROR -> "§b[假人打包] " + playerName + " 发生内部错误，暂停打包";
            default -> null;
        };
        if (message != null) {
            Component text = Component.literal(message);
            server.getPlayerList().getPlayers().forEach(p -> p.sendSystemMessage(text));
            server.sendSystemMessage(text);
        }
    }
}
