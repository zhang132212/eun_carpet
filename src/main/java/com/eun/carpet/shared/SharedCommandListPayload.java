package com.eun.carpet.shared;

import com.eun.carpet.EUNCarpetMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record SharedCommandListPayload(List<SharedCommand> commands) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EUNCarpetMod.MOD_ID, "shared_cmd_list");
    public static final Type<SharedCommandListPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, SharedCommandListPayload> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, SharedCommandListPayload payload) {
            buf.writeVarInt(payload.commands().size());
            for (SharedCommand cmd : payload.commands()) {
                buf.writeUtf(cmd.getId());
                buf.writeUtf(cmd.getName());
                buf.writeVarInt(cmd.getCommands().size());
                for (String line : cmd.getCommands()) buf.writeUtf(line);
                buf.writeUtf(cmd.getDescription());
                buf.writeUUID(cmd.getOwnerUuid());
                buf.writeUtf(cmd.getOwnerName());
                buf.writeLong(cmd.getUploadTime());
                buf.writeLong(cmd.getLastEditTime());
                boolean hasEditor = cmd.getEditorUuid() != null;
                buf.writeBoolean(hasEditor);
                if (hasEditor) {
                    buf.writeUUID(cmd.getEditorUuid());
                    buf.writeUtf(cmd.getEditorName());
                }
            }
        }

        @Override
        public SharedCommandListPayload decode(FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            List<SharedCommand> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                SharedCommand cmd = new SharedCommand();
                cmd.setId(buf.readUtf());
                cmd.setName(buf.readUtf());
                int cmdCount = buf.readVarInt();
                List<String> cmds = new ArrayList<>(cmdCount);
                for (int j = 0; j < cmdCount; j++) cmds.add(buf.readUtf());
                cmd.setCommands(cmds);
                cmd.setDescription(buf.readUtf());
                cmd.setOwnerUuid(buf.readUUID());
                cmd.setOwnerName(buf.readUtf());
                cmd.setUploadTime(buf.readLong());
                cmd.setLastEditTime(buf.readLong());
                if (buf.readBoolean()) {
                    cmd.setEditorUuid(buf.readUUID());
                    cmd.setEditorName(buf.readUtf());
                }
                list.add(cmd);
            }
            return new SharedCommandListPayload(list);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}