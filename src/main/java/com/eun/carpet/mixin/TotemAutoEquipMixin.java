package com.eun.carpet.mixin;

import com.eun.carpet.EUNCarpetSettings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(LivingEntity.class)
public class TotemAutoEquipMixin {

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"))
    private void onCheckTotemDeathProtection(DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!EUNCarpetSettings.autoTotemEquip) return;

        if (!(self instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        if (hasTotemInHand(player)) return;

        ItemStack totem = findTotemInInventory(player);
        if (!totem.isEmpty()) {
            swapWithOffhand(player, totem);
            return;
        }

        ItemStack containerTotem = findTotemInContainers(player);
        if (!containerTotem.isEmpty()) {
            swapWithOffhand(player, containerTotem);
        }
    }

    @Unique
    private boolean hasTotemInHand(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        return mainHand.is(Items.TOTEM_OF_UNDYING) || offHand.is(Items.TOTEM_OF_UNDYING);
    }

    @Unique
    private ItemStack findTotemInInventory(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (i == Inventory.SLOT_OFFHAND) continue;
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                return inv.removeItemNoUpdate(i);
            }
        }
        return ItemStack.EMPTY;
    }

    @Unique
    private ItemStack findTotemInContainers(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (i == Inventory.SLOT_OFFHAND) continue;
            ItemStack containerStack = inv.getItem(i);
            if (containerStack.isEmpty()) continue;

            if (containerStack.has(DataComponents.CONTAINER)) {
                ItemContainerContents contents = containerStack.get(DataComponents.CONTAINER);
                if (contents != null) {
                    List<ItemStack> items = contents.stream().map(ItemStack::copy).toList();
                    for (int slot = 0; slot < items.size(); slot++) {
                        ItemStack item = items.get(slot);
                        if (item.is(Items.TOTEM_OF_UNDYING)) {
                            List<ItemStack> newItems = new ArrayList<>(items);
                            newItems.set(slot, ItemStack.EMPTY);
                            ItemContainerContents newContents = ItemContainerContents.fromItems(newItems);
                            containerStack.set(DataComponents.CONTAINER, newContents);
                            return item;
                        }
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Unique
    private void swapWithOffhand(Player player, ItemStack totem) {
        Inventory inv = player.getInventory();
        ItemStack offhand = inv.getItem(Inventory.SLOT_OFFHAND);
        inv.setItem(Inventory.SLOT_OFFHAND, totem);
        if (!offhand.isEmpty()) {
            if (!inv.add(offhand)) {
                player.drop(offhand, false);
            }
        }
    }
}