package com.eun.carpet.preset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PresetConfig {
    private List<Preset> presets;

    public PresetConfig() {
        this.presets = new ArrayList<>();
    }

    public List<Preset> getPresets() {
        return presets;
    }

    public void setPresets(List<Preset> presets) {
        this.presets = presets;
    }

    public static class Preset {
        private String name;
        private String description;
        private Map<String, String> rules;

        public Preset() {
            this.rules = new HashMap<>();
        }

        public Preset(String name, String description, Map<String, String> rules) {
            this.name = name;
            this.description = description;
            this.rules = rules;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Map<String, String> getRules() { return rules; }
        public void setRules(Map<String, String> rules) { this.rules = rules; }
    }

    public static PresetConfig createDefault() {
        PresetConfig config = new PresetConfig();
        Map<String, String> rules1 = new HashMap<>();
        rules1.put("fakePlayerPersistence", "true");
        rules1.put("fakePlayerActionSaving", "true");
        config.presets.add(new Preset("fake_player_basic", "假人基础配置", rules1));

        Map<String, String> rules2 = new HashMap<>();
        rules2.put("entityOptimizationEnabled", "true");
        config.presets.add(new Preset("performance", "性能优化配置", rules2));

        return config;
    }
}
