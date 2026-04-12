package com.eun.carpet.optimization;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import java.util.Objects;

public final class BlockPosTypeKey {
    private final BlockPos pos;
    private final EntityType<?> type;

    public BlockPosTypeKey(BlockPos pos, EntityType<?> type) {
        this.pos = pos.immutable();
        this.type = type;
    }

    public EntityType<?> getType() { return type; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockPosTypeKey that)) return false;
        return Objects.equals(pos, that.pos) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos, type);
    }
}