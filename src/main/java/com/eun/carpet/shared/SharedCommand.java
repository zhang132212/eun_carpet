package com.eun.carpet.shared;

import java.util.List;
import java.util.UUID;

public class SharedCommand {
    private String id;               // UUID string
    private String name;
    private List<String> commands;   // 命令列表（支持多行）
    private String description;
    private UUID ownerUuid;
    private String ownerName;
    private long uploadTime;
    private long lastEditTime;
    private UUID editorUuid;         // 最后编辑者（可为null）
    private String editorName;       // 最后编辑者名称

    // 必需无参构造器（Gson）
    public SharedCommand() {}

    public SharedCommand(String id, String name, List<String> commands, String description,
                         UUID ownerUuid, String ownerName, long uploadTime) {
        this.id = id;
        this.name = name;
        this.commands = commands;
        this.description = description;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.uploadTime = uploadTime;
        this.lastEditTime = uploadTime;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getCommands() { return commands; }
    public void setCommands(List<String> commands) { this.commands = commands; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public void setOwnerUuid(UUID ownerUuid) { this.ownerUuid = ownerUuid; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public long getUploadTime() { return uploadTime; }
    public void setUploadTime(long uploadTime) { this.uploadTime = uploadTime; }
    public long getLastEditTime() { return lastEditTime; }
    public void setLastEditTime(long lastEditTime) { this.lastEditTime = lastEditTime; }
    public UUID getEditorUuid() { return editorUuid; }
    public void setEditorUuid(UUID editorUuid) { this.editorUuid = editorUuid; }
    public String getEditorName() { return editorName; }
    public void setEditorName(String editorName) { this.editorName = editorName; }
}