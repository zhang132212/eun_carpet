package com.eun.carpet.optimization;

import com.eun.carpet.EUNCarpetMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HideEntitiesPayload(int[] added, int[] removed) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EUNCarpetMod.MOD_ID, "hide_entities");
    public static final Type<HideEntitiesPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, HideEntitiesPayload> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, HideEntitiesPayload payload) {
            buf.writeVarInt(payload.added.length);
            for (int id : payload.added) buf.writeVarInt(id);
            buf.writeVarInt(payload.removed.length);
            for (int id : payload.removed) buf.writeVarInt(id);
        }

        @Override
        public HideEntitiesPayload decode(FriendlyByteBuf buf) {
            int addedLen = buf.readVarInt();
            int[] added = new int[addedLen];
            for (int i = 0; i < addedLen; i++) added[i] = buf.readVarInt();

            int removedLen = buf.readVarInt();
            int[] removed = new int[removedLen];
            for (int i = 0; i < removedLen; i++) removed[i] = buf.readVarInt();

            return new HideEntitiesPayload(added, removed);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}