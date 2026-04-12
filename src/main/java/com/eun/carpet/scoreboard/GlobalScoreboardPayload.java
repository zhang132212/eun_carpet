package com.eun.carpet.scoreboard;

import com.eun.carpet.EUNCarpetMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class GlobalScoreboardPayload implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EUNCarpetMod.MOD_ID, "global_scoreboard");
    public static final Type<GlobalScoreboardPayload> TYPE = new Type<>(ID);

    private final Map<ScoreBoardType, List<RankEntry>> rankings;

    public GlobalScoreboardPayload(Map<ScoreBoardType, List<RankEntry>> rankings) {
        this.rankings = rankings;
    }

    public static final StreamCodec<FriendlyByteBuf, GlobalScoreboardPayload> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, GlobalScoreboardPayload payload) {
            buf.writeVarInt(payload.rankings.size());
            for (Map.Entry<ScoreBoardType, List<RankEntry>> entry : payload.rankings.entrySet()) {
                buf.writeEnum(entry.getKey());
                List<RankEntry> list = entry.getValue();
                buf.writeVarInt(list.size());
                for (RankEntry re : list) {
                    buf.writeUtf(re.playerName());
                    buf.writeInt(re.value());
                }
            }
        }

        @Override
        public GlobalScoreboardPayload decode(FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            Map<ScoreBoardType, List<RankEntry>> map = new EnumMap<>(ScoreBoardType.class);
            for (int i = 0; i < size; i++) {
                ScoreBoardType type = buf.readEnum(ScoreBoardType.class);
                int listSize = buf.readVarInt();
                List<RankEntry> list = new ArrayList<>(listSize);
                for (int j = 0; j < listSize; j++) {
                    String name = buf.readUtf();
                    int value = buf.readInt();
                    list.add(new RankEntry(name, value));
                }
                map.put(type, list);
            }
            return new GlobalScoreboardPayload(map);
        }
    };

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record RankEntry(String playerName, int value) {}
}