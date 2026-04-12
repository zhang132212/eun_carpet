package com.eun.carpet.highlight;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HighlightPayload(boolean highlightItems, boolean highlightEntities, int itemColor, int entityColor) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<HighlightPayload> TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath("eun_carpet", "highlight")
    );
    public static final StreamCodec<FriendlyByteBuf, HighlightPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, HighlightPayload::highlightItems,
            ByteBufCodecs.BOOL, HighlightPayload::highlightEntities,
            ByteBufCodecs.INT, HighlightPayload::itemColor,
            ByteBufCodecs.INT, HighlightPayload::entityColor,
            HighlightPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}