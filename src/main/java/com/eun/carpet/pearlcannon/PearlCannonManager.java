package com.eun.carpet.pearlcannon;

import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PearlCannonManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("EUNCarpet|PearlCannon");
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("eun_carpet/pearlCannon");
    private static final String DEFAULT_SCHEME = "default.json";

    private static PearlCannonManager instance;
    private final Map<String, PearlCannonScheme> schemes = new ConcurrentHashMap<>();

    private PearlCannonManager() {
        ensureConfigDir();
        loadAll();
    }

    public static PearlCannonManager getInstance() {
        if (instance == null) {
            instance = new PearlCannonManager();
        }
        return instance;
    }

    private void ensureConfigDir() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
                createDefaultScheme();
            } else {
                // 如果目录存在但为空，也创建默认文件
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(CONFIG_DIR, "*.json")) {
                    if (!stream.iterator().hasNext()) {
                        createDefaultScheme();
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("无法创建配置目录", e);
        }
    }

    private void createDefaultScheme() {
        Path defaultPath = CONFIG_DIR.resolve(DEFAULT_SCHEME);
        if (!Files.exists(defaultPath)) {
            String defaultContent = """
                    {
                      "_comment": "珍珠炮配置文件模板。totalBits：所有字段位数总和；fields：字段定义列表；outputOrder：输出二进制串的拼接顺序（按字段名）。",
                      "totalBits": 33,
                      "fields": [
                        {
                          "_comment": "SUM类型字段：蓝色TNT。用户输入一个整数，按权重列表分解为二进制。权重列表长度必须等于bits，且能表示0到所有权重和之间的任意整数。",
                          "name": "blue",
                          "displayName": "蓝色TNT",
                          "type": "sum",
                          "bits": 15,
                          "weights": [1, 2, 3, 4, 10, 20, 40, 80, 160, 300, 600, 1200, 2400, 4800, 6000]
                        },
                        {
                          "_comment": "注意:除了蓝色tnt和红色tnt的配置项目以外，其余的项目均为自由配置项(如方向，是否抛射等等)可以去掉，并非一定要加。\\nENUM类型字段:用户输入一个预定义的字符串，映射为固定长度的二进制串。映射值必须为0或1组成的字符串，长度等于bits。\\nname:基本名字，不能是中文。\\ntype:分为enum和sum。enum类型字段:用户输入一个预定义的字符串，映射为固定长度的二进制串。映射值必须为0或1组成的字符串，长度等于bits；sum类型字段：用户输入一个整数，按权重列表分解为二进制。权重列表长度必须等于bits，且能表示0到所有权重和之间的任意整数。(自由配置项一律为enum，红蓝tnt一律为sum)。\\ndisplayName:可以是中文，用于输入错误之后的命令提示。\\nbits:该配置项的最大字符串长度。\\nmapping:对于这个组的所有可能情况,只有enum配置项需要。",
                          "name": "direction",
                          "displayName": "方向",
                          "type": "enum",
                          "bits": 2,
                          "mapping": {
                            "east": "11",
                            "north": "10",
                            "south": "01",
                            "west": "00"
                          }
                        },
                        {
                          "name": "launch",
                          "displayName": "抛射",
                          "type": "enum",
                          "bits": 1,
                          "mapping": {
                            "enable": "0",
                            "disable": "1"
                          }
                        },
                        {
                          "_comment": "SUM类型字段：红色TNT。",
                          "name": "red",
                          "displayName": "红色TNT",
                          "type": "sum",
                          "bits": 15,
                          "weights": [6000, 4800, 2400, 1200, 600, 300, 160, 80, 40, 20, 10, 4, 3, 2, 1]
                        }
                      ],
                      "outputOrder": ["blue", "direction", "launch", "red"],
                      "_comment_end": "outputOrder:这里是输出编码的顺序。"
                    }
                    """;
            try {
                Files.writeString(defaultPath, defaultContent);
                LOGGER.info("已创建默认珍珠炮配置文件: {}", defaultPath);
            } catch (IOException e) {
                LOGGER.error("无法写入默认配置文件", e);
            }
        }
    }

    public void loadAll() {
        schemes.clear();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(CONFIG_DIR, "*.json")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                try {
                    String content = Files.readString(path);
                    PearlCannonScheme scheme = PearlCannonScheme.fromJson(fileName, content);
                    schemes.put(scheme.getName(), scheme);
                    LOGGER.info("加载珍珠炮方案: {}", scheme.getName());
                } catch (JsonParseException e) {
                    LOGGER.error("解析配置文件 {} 失败: {}", fileName, e.getMessage());
                } catch (IOException e) {
                    LOGGER.error("读取配置文件 {} 失败", fileName, e);
                }
            }
        } catch (IOException e) {
            LOGGER.error("扫描配置目录失败", e);
        }
    }

    public PearlCannonScheme getScheme(String name) {
        return schemes.get(name);
    }

    public Set<String> getSchemeNames() {
        return schemes.keySet();
    }

    public void reload() {
        loadAll();
    }
}