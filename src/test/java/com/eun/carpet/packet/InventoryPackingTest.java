package com.eun.carpet.packet;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryPackingTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // 26.2 binds default item components separately from static registries.
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup())
                .forEach(DataComponentInitializers.PendingComponents::apply);
    }

    @Test
    void staleCountAfterMakingSpaceCannotDuplicateItems() {
        ItemStack[] slots = emptySlots();
        // Before making space there were 1,600 items; three 64-stacks were dropped.
        for (int i = 0; i < 22; i++) slots[i] = new ItemStack(Items.COBBLESTONE, 64);
        List<ItemStack> contents = InventoryPacking.takeForBox(inventory(slots), Items.COBBLESTONE, 1600);

        assertEquals(1408, count(contents));
        assertEquals(1600, count(contents) + count(Arrays.asList(slots)) + 192);
        assertEquals(0, count(Arrays.asList(slots)));
    }

    @Test
    void boxCapacityLeavesExcessInInventory() {
        ItemStack[] slots = emptySlots();
        for (int i = 0; i < 36; i++) slots[i] = new ItemStack(Items.COBBLESTONE, 64);
        List<ItemStack> contents = InventoryPacking.takeForBox(inventory(slots), Items.COBBLESTONE, 2304);

        assertEquals(27, contents.size());
        assertEquals(1728, count(contents));
        assertEquals(576, count(Arrays.asList(slots)));
    }

    @Test
    void sixteenStackItemsUseTheirActualCapacity() {
        ItemStack[] slots = emptySlots();
        for (int i = 0; i < 36; i++) slots[i] = new ItemStack(Items.ENDER_PEARL, 16);
        List<ItemStack> contents = InventoryPacking.takeForBox(inventory(slots), Items.ENDER_PEARL, 576);

        assertEquals(27, contents.size());
        assertTrue(contents.stream().allMatch(stack -> stack.getCount() <= stack.getMaxStackSize()));
        assertEquals(432, count(contents));
        assertEquals(144, count(Arrays.asList(slots)));
    }

    @Test
    void absentTypeCannotCreateItemsAndOtherTypesAreUntouched() {
        ItemStack[] slots = emptySlots();
        slots[0] = new ItemStack(Items.STONE, 12);
        assertTrue(InventoryPacking.takeForBox(inventory(slots), Items.COBBLESTONE, 64).isEmpty());
        assertEquals(12, slots[0].getCount());
    }

    @Test
    void requestedCountLeavesRemainderAndUnrelatedItemsUntouched() {
        ItemStack[] slots = emptySlots();
        slots[0] = new ItemStack(Items.COBBLESTONE, 64);
        slots[1] = new ItemStack(Items.STONE, 12);
        slots[2] = new ItemStack(Items.COBBLESTONE, 64);
        List<ItemStack> contents = InventoryPacking.takeForBox(inventory(slots), Items.COBBLESTONE, 70);

        assertEquals(70, count(contents));
        assertTrue(slots[0].isEmpty());
        assertEquals(12, slots[1].getCount());
        assertEquals(58, slots[2].getCount());
        assertEquals(140, count(contents) + count(Arrays.asList(slots)));
    }

    private static ItemStack[] emptySlots() {
        ItemStack[] slots = new ItemStack[36];
        Arrays.fill(slots, ItemStack.EMPTY);
        return slots;
    }

    private static Inventory inventory(ItemStack[] slots) {
        Inventory inventory = mock(Inventory.class);
        when(inventory.getItem(anyInt())).thenAnswer(call -> slots[call.getArgument(0)]);
        doAnswer(call -> {
            slots[call.getArgument(0)] = call.getArgument(1);
            return null;
        }).when(inventory).setItem(anyInt(), any(ItemStack.class));
        return inventory;
    }

    private static int count(List<ItemStack> stacks) {
        return stacks.stream().mapToInt(ItemStack::getCount).sum();
    }
}
