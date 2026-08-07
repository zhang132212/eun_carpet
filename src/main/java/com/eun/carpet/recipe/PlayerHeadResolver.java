package com.eun.carpet.recipe;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家头颅 Profile 解析器: 名字 -> 完整 GameProfile (含 textures 皮肤数据)。
 * 26.2 移除了 MinecraftServer.getProfileCache()/getSessionService(),
 * 这里直接 HTTP 调用 Mojang 官方 API, 自包含不依赖 MC 内部结构。
 */
public class PlayerHeadResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger("EUNCarpet|PlayerHead");

    private static final ConcurrentHashMap<String, GameProfile> FULL_PROFILE_CACHE = new ConcurrentHashMap<>();

    private static final int TIMEOUT_MS = 3000;
    private static final String UUID_API = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SESSION_API = "https://sessionserver.mojang.com/session/minecraft/profile/";

    /** 合成用同步解析: 缓存命中秒回; 未命中联网查询; 失败/不存在返回 null */
    public static GameProfile resolveFullSync(String name, MinecraftServer server) {
        if (name == null || name.isEmpty()) return null;
        String key = name.toLowerCase();
        GameProfile cached = FULL_PROFILE_CACHE.get(key);
        if (cached != null) return cached;
        if (server != null && !server.usesAuthentication()) return null; // 离线模式不联网
        try {
            GameProfile profile = fetchFullProfile(name);
            if (profile != null) {
                FULL_PROFILE_CACHE.put(key, profile);
                LOGGER.info("[PlayerHead] 解析成功: {} -> {} (纹理={})", name, profile.id(), profile.properties().containsKey("textures"));
                return profile;
            }
            LOGGER.info("[PlayerHead] 未找到玩家: {}", name);
        } catch (Exception e) {
            LOGGER.warn("[PlayerHead] 解析失败 {}: {}", name, e.toString());
        }
        return null;
    }

    /** 异步解析(保留给后续指令使用) */
    public static CompletableFuture<GameProfile> resolveFullAsync(String name, MinecraftServer server) {
        String key = name == null ? "" : name.toLowerCase();
        GameProfile cached = FULL_PROFILE_CACHE.get(key);
        if (cached != null) return CompletableFuture.completedFuture(cached);
        return CompletableFuture.supplyAsync(() -> resolveFullSync(name, server));
    }

    private static GameProfile fetchFullProfile(String name) throws Exception {
        String uuidStr = fetchUuid(name);
        if (uuidStr == null) return null;
        // Mojang API 返回无横线 UUID, UUID.fromString 要求 8-4-4-4-12 格式
        UUID uuid = UUID.fromString(uuidStr.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
        String textures = fetchTextures(uuidStr);
        if (textures != null) {
            // 26.2 authlib: PropertyMap 构造时强制 ImmutableMultimap.copyOf, 无法事后修改;
            // 必须先构建带 textures 的 Multimap 再经 PropertyMap 传给 GameProfile 三参构造
            Multimap<String, Property> mm = ArrayListMultimap.create();
            mm.put("textures", new Property("textures", textures));
            return new GameProfile(uuid, name, new PropertyMap(mm));
        }
        return new GameProfile(uuid, name);
    }

    private static String fetchUuid(String name) throws Exception {
        HttpURLConnection conn = open(UUID_API + name);
        int code = conn.getResponseCode();
        if (code == 200) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                if (obj.has("id")) return obj.get("id").getAsString();
            }
        } else {
            conn.disconnect();
        }
        return null;
    }

    private static String fetchTextures(String uuid) throws Exception {
        HttpURLConnection conn = open(SESSION_API + uuid);
        int code = conn.getResponseCode();
        if (code == 200) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                if (obj.has("properties")) {
                    for (var el : obj.getAsJsonArray("properties")) {
                        JsonObject prop = el.getAsJsonObject();
                        if ("textures".equals(prop.get("name").getAsString()) && prop.has("value")) {
                            return prop.get("value").getAsString();
                        }
                    }
                }
            }
        } else {
            conn.disconnect();
        }
        return null;
    }

    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URI(url).toURL().openConnection();
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "EUNCarpet/1.0");
        return conn;
    }
}
