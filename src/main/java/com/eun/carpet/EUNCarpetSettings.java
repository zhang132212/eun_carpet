package com.eun.carpet;

import carpet.api.settings.Rule;
import carpet.api.settings.RuleCategory;

public class EUNCarpetSettings {
    public static final String EUN = "eun";

    @Rule(categories = {RuleCategory.FEATURE, EUN})
    public static boolean fakePlayerPersistence = true;

    @Rule(categories = {RuleCategory.FEATURE, EUN})
    public static boolean fakePlayerActionSaving = false;

    @Rule(categories = {RuleCategory.OPTIMIZATION, EUN})
    public static boolean entityOptimizationEnabled = true;

    @Rule(categories = {RuleCategory.FEATURE, EUN}, options = {"false", "true", "0", "1", "2", "3", "4"})
    public static String pearlCannonEnabled = "true";

    @Rule(categories = {RuleCategory.FEATURE, EUN})
    public static boolean fakePlayerPrefix = false;

    @Rule(categories = {RuleCategory.FEATURE, EUN})
    public static boolean globalScoreboardEnabled = false;

    @Rule(categories = {RuleCategory.FEATURE, EUN}, options = {"false", "true", "0", "1", "2", "3", "4"})
    public static String highlightEnabled = "true";

    @Rule(categories = {RuleCategory.FEATURE, EUN})
    public static boolean craftableInvisibleItemFrames = false;

    @Rule(categories = {RuleCategory.FEATURE, EUN})
    public static boolean craftablePlayerHeads = true;

    @Rule(categories = {RuleCategory.FEATURE, EUN})
    public static boolean autoTotemEquip = false;

    @Rule(categories = {RuleCategory.FEATURE, EUN})
    public static boolean suppressMagmaCubeInNetherWastes = false;

    @Rule(categories = {RuleCategory.FEATURE, EUN}, options = {"false", "true", "0", "1", "2", "3", "4"})
    public static String packetEnabled = "true";

    @Rule(categories = {RuleCategory.FEATURE, EUN}, options = {"false", "true", "0", "1", "2", "3", "4"})
    public static String presetEnabled = "true";

    @Rule(categories = {RuleCategory.FEATURE, EUN})
    public static boolean sharedCommandsEnabled = true;

    @Rule(categories = {RuleCategory.COMMAND, EUN}, options = {"true", "false", "ops", "0", "1", "2", "3", "4"})
    public static String sharedCommandManagePermission = "ops";

    // 已移除 sharedCommandBlacklist 规则

    @Rule(categories = {RuleCategory.COMMAND, EUN})
    public static int sharedCommandMaxLength = 256;
}
