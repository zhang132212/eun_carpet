package com.eun.carpet.shared;

import com.eun.carpet.EUNCarpetMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record UploadSharedCommandPayload(String name, List<String> commands, String description) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EUNCarpetMod.MOD_ID, "upload_shared_cmd");
    public static final Type<UploadSharedCommandPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, UploadSharedCommandPayload> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, UploadSharedCommandPayload payload) {
            buf.writeUtf(payload.name());
            buf.writeVarInt(payload.commands().size());
            for (String cmd : payload.commands()) buf.writeUtf(cmd);
            buf.writeUtf(payload.description());
        }

        @Override
        public UploadSharedCommandPayload decode(FriendlyByteBuf buf) {
            String name = buf.readUtf();
            int size = buf.readVarInt();
            List<String> cmds = new java.util.ArrayList<>(size);
            for (int i = 0; i < size; i++) cmds.add(buf.readUtf());
            String desc = buf.readUtf();
            return new UploadSharedCommandPayload(name, cmds, desc);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}