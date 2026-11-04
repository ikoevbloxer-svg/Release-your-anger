package com.example.crackwhip;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Сервер -> клиент: «сущность entityId привязана к holderId» (holderId = -1 — не привязана). */
public record ChainSyncPayload(int entityId, int holderId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ChainSyncPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(CrackWhip.MODID, "chain_sync"));

    public static final StreamCodec<ByteBuf, ChainSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ChainSyncPayload::entityId,
            ByteBufCodecs.VAR_INT, ChainSyncPayload::holderId,
            ChainSyncPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> ClientChains.set(payload.entityId(), payload.holderId())));
    }
}
