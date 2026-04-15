package com.eun.carpet.shared;

import com.eun.carpet.EUNCarpetMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UploadResultPayload(boolean success, String message, String commandId) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EUNCarpetMod.MOD_ID, "upload_result");
    public static final Type<UploadResultPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, UploadResultPayload> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, UploadResultPayload payload) {
            buf.writeBoolean(payload.success());
            buf.writeUtf(payload.message());
            buf.writeUtf(payload.commandId());
        }
        @Override
        public UploadResultPayload decode(FriendlyByteBuf buf) {
            return new UploadResultPayload(buf.readBoolean(), buf.readUtf(), buf.readUtf());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}