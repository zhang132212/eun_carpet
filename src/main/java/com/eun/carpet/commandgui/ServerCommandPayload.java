package com.eun.carpet.commandgui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.List;

/**
 * 服务端 → 客户端数据包，将服务器配置的指令预设发送给安装了 easy-cmd 的客户端。
 * <p>
 * 二进制格式与 easy-cmd {@code com.remrin.sync.ServerCommandPayload.CODEC} 保持一致：
 * <pre>
 *   VarInt  组数量
 *   for each 组:
 *     UTF     组名
 *     VarInt  条目数量
 *     for each 条目:
 *       UTF     名称
 *       UTF     描述
 *       VarInt  指令数量
 *       UTF*    指令列表
 * </pre>
 * 频道标识：{@code eun_carpet:cmd_gui_preset}
 */
public record ServerCommandPayload(List<ServerCommandConfigManager.GroupDto> groups)
		implements CustomPacketPayload {

	public static final Identifier ID = Identifier.fromNamespaceAndPath("eun_carpet", "cmd_gui_preset");
	public static final Type<ServerCommandPayload> TYPE = new Type<>(ID);

	public static final StreamCodec<FriendlyByteBuf, ServerCommandPayload> CODEC = new StreamCodec<>() {
		@Override
		public void encode(FriendlyByteBuf buf, ServerCommandPayload payload) {
			List<ServerCommandConfigManager.GroupDto> groups = payload.groups();
			buf.writeVarInt(groups.size());
			for (ServerCommandConfigManager.GroupDto group : groups) {
				buf.writeUtf(str(group.name));
				List<ServerCommandConfigManager.EntryDto> entries = nullSafe(group.commands);
				buf.writeVarInt(entries.size());
				for (ServerCommandConfigManager.EntryDto entry : entries) {
					buf.writeUtf(str(entry.name));
					buf.writeUtf(str(entry.description));
					List<String> cmds = nullSafe(entry.commands);
					buf.writeVarInt(cmds.size());
					for (String cmd : cmds) buf.writeUtf(cmd);
				}
			}
		}

		@Override
		public ServerCommandPayload decode(FriendlyByteBuf buf) {
			// 仅服务端发送，客户端无需解码
			throw new UnsupportedOperationException("仅服务端发送");
		}
	};

	private static String str(String s) {
		return s != null ? s : "";
	}

	private static <T> List<T> nullSafe(List<T> list) {
		return list != null ? list : Collections.emptyList();
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
