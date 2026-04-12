package com.eun.carpet.scoreboard;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public enum ScoreBoardType {
    MINING("挖掘") {
        @Override
        public int getValue(StatsCounter stats) {
            int total = 0;
            for (Block block : BuiltInRegistries.BLOCK) {
                Stat<Block> stat = Stats.BLOCK_MINED.get(block, StatFormatter.DEFAULT);
                total += stats.getValue(stat);
            }
            return total;
        }
    },
    BUILDING("建造") {
        @Override
        public int getValue(StatsCounter stats) {
            int total = 0;
            for (Item item : BuiltInRegistries.ITEM) {
                Stat<Item> stat = Stats.ITEM_USED.get(item, StatFormatter.DEFAULT);
                total += stats.getValue(stat);
            }
            return total;
        }
    },
    KILL("击杀") {
        @Override
        public int getValue(StatsCounter stats) {
            int total = 0;
            for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
                Stat<EntityType<?>> stat = Stats.ENTITY_KILLED.get(entityType, StatFormatter.DEFAULT);
                total += stats.getValue(stat);
            }
            return total;
        }
    },
    DAMAGE_DEALT("伤害输出") {
        @Override
        public int getValue(StatsCounter stats) {
            return stats.getValue(Stats.CUSTOM.get(Stats.DAMAGE_DEALT, StatFormatter.DIVIDE_BY_TEN));
        }
    },
    DAMAGE_TAKEN("承受伤害") {
        @Override
        public int getValue(StatsCounter stats) {
            return stats.getValue(Stats.CUSTOM.get(Stats.DAMAGE_TAKEN, StatFormatter.DIVIDE_BY_TEN));
        }
    },
    FLY("飞行距离") {
        @Override
        public int getValue(StatsCounter stats) {
            return stats.getValue(Stats.CUSTOM.get(Stats.AVIATE_ONE_CM, StatFormatter.DISTANCE));
        }
    },
    TRADE("交易次数") {
        @Override
        public int getValue(StatsCounter stats) {
            return stats.getValue(Stats.CUSTOM.get(Stats.TRADED_WITH_VILLAGER, StatFormatter.DEFAULT));
        }
    },
    DEATH("死亡次数") {
        @Override
        public int getValue(StatsCounter stats) {
            return stats.getValue(Stats.CUSTOM.get(Stats.DEATHS, StatFormatter.DEFAULT));
        }
    },
    FISH("钓鱼次数") {
        @Override
        public int getValue(StatsCounter stats) {
            return stats.getValue(Stats.CUSTOM.get(Stats.FISH_CAUGHT, StatFormatter.DEFAULT));
        }
    },
    PLAY_TIME("在线时长") {
        @Override
        public int getValue(StatsCounter stats) {
            return stats.getValue(Stats.CUSTOM.get(Stats.PLAY_TIME, StatFormatter.TIME));
        }
    };

    public final String displayName;

    ScoreBoardType(String displayName) {
        this.displayName = displayName;
    }

    public abstract int getValue(StatsCounter stats);
}