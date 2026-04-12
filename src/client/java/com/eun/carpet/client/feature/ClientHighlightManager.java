package com.eun.carpet.client.feature;

public class ClientHighlightManager {
    public static final ClientHighlightManager INSTANCE = new ClientHighlightManager();

    private boolean highlightItems = false;
    private boolean highlightEntities = false;
    private int itemColor = 0xFFFFFFFF;
    private int entityColor = 0xFFFFFFFF;

    public void update(boolean items, boolean entities, int itemColor, int entityColor) {
        this.highlightItems = items;
        this.highlightEntities = entities;
        this.itemColor = itemColor;
        this.entityColor = entityColor;
    }

    public boolean shouldHighlightItems() {
        return highlightItems;
    }

    public boolean shouldHighlightEntities() {
        return highlightEntities;
    }

    public int getItemColor() {
        return itemColor;
    }

    public int getEntityColor() {
        return entityColor;
    }
}