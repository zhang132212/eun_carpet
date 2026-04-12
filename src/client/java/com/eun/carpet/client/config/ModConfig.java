package com.eun.carpet.client.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

@Config(name = "eun_carpet")
public class ModConfig implements ConfigData {
    @ConfigEntry.Gui.Tooltip
    public boolean enableOptimization = true;

    @ConfigEntry.Gui.Tooltip
    public List<String> excludedEntityTypes = new ArrayList<>();

    @ConfigEntry.Gui.Tooltip
    public boolean hideDebugHitboxes = false;
}