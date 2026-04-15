package com.eun.carpet.shared;

import com.eun.carpet.EUNCarpetMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestSharedCommandListPayload() implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EUNCarpetMod.MOD_ID, "req_shared_cmd_list");
    public static final Type<RequestSharedCommandListPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, RequestSharedCommandListPayload> CODEC = StreamCodec.unit(new RequestSharedCommandListPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}