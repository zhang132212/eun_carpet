package com.eun.carpet.shared;

import com.eun.carpet.EUNCarpetMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record SharedCommandUpdatePayload(String action, SharedCommand command) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EUNCarpetMod.MOD_ID, "shared_cmd_update");
    public static final Type<SharedCommandUpdatePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, SharedCommandUpdatePayload> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, SharedCommandUpdatePayload payload) {
            buf.writeUtf(payload.action());
            SharedCommand cmd = payload.command();
            buf.writeUtf(cmd.getId());
            if ("REMOVED".equals(payload.action())) return;
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

        @Override
        public SharedCommandUpdatePayload decode(FriendlyByteBuf buf) {
            String action = buf.readUtf();
            String id = buf.readUtf();
            if ("REMOVED".equals(action)) {
                SharedCommand dummy = new SharedCommand();
                dummy.setId(id);
                return new SharedCommandUpdatePayload(action, dummy);
            }
            SharedCommand cmd = new SharedCommand();
            cmd.setId(id);
            cmd.setName(buf.readUtf());
            int cmdCount = buf.readVarInt();
            List<String> cmds = new ArrayList<>(cmdCount);
            for (int i = 0; i < cmdCount; i++) cmds.add(buf.readUtf());
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
            return new SharedCommandUpdatePayload(action, cmd);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}