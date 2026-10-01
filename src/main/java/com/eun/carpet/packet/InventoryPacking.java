package com.eun.carpet.packet;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Moves only available inventory items into at most 27 legal-sized box stacks. */
final class InventoryPacking {
    private static final int MAIN_SIZE = 36;
    private static final int BOX_SIZE = 27;

    private InventoryPacking() {}

    static List<ItemStack> takeForBox(Inventory inventory, Item item, long requested) {
        int maxStackSize = item.getDefaultMaxStackSize();
        long remaining = Math.min(requested, (long) BOX_SIZE * maxStackSize);
        long removed = 0;
        for (int slot = 0; slot < MAIN_SIZE && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !stack.is(item)) continue;
            int count = (int) Math.min(remaining, stack.getCount());
            stack.shrink(count);
            removed += count;
            remaining -= count;
            if (stack.isEmpty()) inventory.setItem(slot, ItemStack.EMPTY);
        }

        List<ItemStack> contents = new ArrayList<>();
        while (removed > 0) {
            int count = (int) Math.min(removed, maxStackSize);
            contents.add(new ItemStack(item, count));
            removed -= count;
        }
        return contents;
    }
}
