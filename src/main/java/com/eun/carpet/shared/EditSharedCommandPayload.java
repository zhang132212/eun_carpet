package com.eun.carpet.shared;

import com.eun.carpet.EUNCarpetMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record EditSharedCommandPayload(String id, String name, List<String> commands, String description) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EUNCarpetMod.MOD_ID, "edit_shared_cmd");
    public static final Type<EditSharedCommandPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, EditSharedCommandPayload> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, EditSharedCommandPayload payload) {
            buf.writeUtf(payload.id());
            buf.writeUtf(payload.name());
            buf.writeVarInt(payload.commands().size());
            for (String cmd : payload.commands()) buf.writeUtf(cmd);
            buf.writeUtf(payload.description());
        }

        @Override
        public EditSharedCommandPayload decode(FriendlyByteBuf buf) {
            String id = buf.readUtf();
            String name = buf.readUtf();
            int size = buf.readVarInt();
            List<String> cmds = new java.util.ArrayList<>(size);
            for (int i = 0; i < size; i++) cmds.add(buf.readUtf());
            String desc = buf.readUtf();
            return new EditSharedCommandPayload(id, name, cmds, desc);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}