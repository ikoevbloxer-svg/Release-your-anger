package com.example.crackwhip;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CrackWhip.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CrackWhip.MODID);

    public static final DeferredItem<WhipItem> WHIP = ITEMS.register("whip",
            () -> new WhipItem(new Item.Properties()
                    .durability(600)
                    .attributes(WhipItem.createAttributes())));

    // Цепь не ломается и не горит
    public static final DeferredItem<ChainLeashItem> CHAIN_LEASH = ITEMS.register("chain_leash",
            () -> new ChainLeashItem(new Item.Properties().stacksTo(1).fireResistant()));

    public static final Supplier<CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.crackwhip"))
                    .icon(() -> WHIP.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(WHIP.get());
                        output.accept(CHAIN_LEASH.get());
                    })
                    .build());
}
