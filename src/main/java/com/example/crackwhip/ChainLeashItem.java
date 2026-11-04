package com.example.crackwhip;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Вся логика — в {@link Chains}: нужно перехватывать ПКМ раньше, чем сработает жителя/волк/лошадь. */
public class ChainLeashItem extends Item {
    public ChainLeashItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.crackwhip.chain_leash.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.crackwhip.chain_leash.tooltip2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
