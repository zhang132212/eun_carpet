package com.eun.carpet.shared;

import com.eun.carpet.EUNCarpetMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DeleteSharedCommandPayload(String id) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EUNCarpetMod.MOD_ID, "delete_shared_cmd");
    public static final Type<DeleteSharedCommandPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, DeleteSharedCommandPayload> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, DeleteSharedCommandPayload payload) {
            buf.writeUtf(payload.id());
        }
        @Override
        public DeleteSharedCommandPayload decode(FriendlyByteBuf buf) {
            return new DeleteSharedCommandPayload(buf.readUtf());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}