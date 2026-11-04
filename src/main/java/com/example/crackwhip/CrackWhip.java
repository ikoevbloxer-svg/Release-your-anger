package com.example.crackwhip;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(CrackWhip.MODID)
public class CrackWhip {
    public static final String MODID = "crackwhip";

    public CrackWhip(IEventBus modBus) {
        ModItems.ITEMS.register(modBus);
        ModItems.TABS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        modBus.addListener(ChainSyncPayload::register);
    }
}
