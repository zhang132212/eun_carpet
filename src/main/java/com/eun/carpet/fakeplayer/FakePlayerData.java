package com.eun.carpet.fakeplayer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public class FakePlayerData {
    private UUID uuid;
    private String name;
    private double x, y, z;
    private float yaw, pitch;
    private String gameMode;
    private String dimension; // 存储为字符串，例如 "minecraft:overworld"

    private boolean sneaking;
    private boolean sprinting;
    private float forward;
    private float strafing;
    private List<SavedAction> actions;

    public FakePlayerData() {}

    public FakePlayerData(UUID uuid, String name, Vec3 pos, float yaw, float pitch, GameType gameMode, ResourceKey<Level> dimension) {
        this.uuid = uuid;
        this.name = name;
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.gameMode = gameMode.getName();
        this.dimension = dimension.identifier().toString(); // 存储为字符串
    }

    public void updateActionsFrom(FakePlayerData other) {
        this.sneaking = other.sneaking;
        this.sprinting = other.sprinting;
        this.forward = other.forward;
        this.strafing = other.strafing;
        this.actions = other.actions;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public Vec3 getPosition() { return new Vec3(x, y, z); }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public GameType getGameMode() {
        return GameType.byName(gameMode, GameType.SURVIVAL);
    }

    public ResourceKey<Level> getDimension() {
        if (dimension == null) return Level.OVERWORLD;
        return ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimension));
    }

    public void setDimension(ResourceKey<Level> dim) {
        this.dimension = dim.identifier().toString();
    }

    // 为 Gson 保留的 getter/setter
    public String getDimensionString() { return dimension; }
    public void setDimensionString(String dim) { this.dimension = dim; }

    public boolean isSneaking() { return sneaking; }
    public void setSneaking(boolean sneaking) { this.sneaking = sneaking; }
    public boolean isSprinting() { return sprinting; }
    public void setSprinting(boolean sprinting) { this.sprinting = sprinting; }
    public float getForward() { return forward; }
    public void setForward(float forward) { this.forward = forward; }
    public float getStrafing() { return strafing; }
    public void setStrafing(float strafing) { this.strafing = strafing; }
    public List<SavedAction> getActions() { return actions; }
    public void setActions(List<SavedAction> actions) { this.actions = actions; }

    public static class SavedAction {
        private String type;
        private int limit;
        private int interval;
        private int offset;
        private boolean continuous;

        public SavedAction() {}
        public SavedAction(String type, int limit, int interval, int offset, boolean continuous) {
            this.type = type;
            this.limit = limit;
            this.interval = interval;
            this.offset = offset;
            this.continuous = continuous;
        }
        public String getType() { return type; }
        public int getLimit() { return limit; }
        public int getInterval() { return interval; }
        public int getOffset() { return offset; }
        public boolean isContinuous() { return continuous; }
    }
}