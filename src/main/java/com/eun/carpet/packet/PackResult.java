package com.eun.carpet.packet;

public enum PackResult {
    UNKNOWN,
    SUCCESS,
    NO_CRAFTING_TABLE,
    NO_LOG_BOX,
    NO_SHELL_BOX,
    MIXED_LOG_TYPES,
    INSUFFICIENT_MATERIALS,
    INSUFFICIENT_SPACE,
    INTERNAL_ERROR
}