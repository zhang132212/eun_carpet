package com.eun.carpet.packet;

public class PacketConfig {
    private String name;          // 假人名称
    private int type;             // 打包物品种类数 1-5
    private boolean enabled;      // 是否启用
    private transient int lastTick; // 上次执行刻（不持久化）
    private transient PackResult lastResult; // 上次执行结果

    public PacketConfig() {}

    public PacketConfig(String name, int type, boolean enabled) {
        this.name = name;
        this.type = type;
        this.enabled = enabled;
        this.lastTick = 0;
        this.lastResult = PackResult.UNKNOWN;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getType() { return type; }
    public void setType(int type) { this.type = type; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getLastTick() { return lastTick; }
    public void setLastTick(int tick) { this.lastTick = tick; }
    public PackResult getLastResult() { return lastResult; }
    public void setLastResult(PackResult result) { this.lastResult = result; }
}