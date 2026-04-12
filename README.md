# EUN Carpet Addition

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.11-blue.svg)](https://minecraft.net)
[![Fabric API](https://img.shields.io/badge/Fabric%20API-0.141.3%2B1.21.11-yellow.svg)](https://fabricmc.net)

**EUN Carpet Addition** 是一个基于 [Fabric Carpet](https://github.com/gnembon/fabric-carpet) 的扩展模组，为原版游戏添加了一系列实用功能，包括假人驻留与自动打包、实体渲染优化、个人高亮、珍珠炮编码工具、全局计分板等。

---

### 安装位置
- **服务端**：必须安装本模组（所有功能均由服务端驱动）。
- **客户端**：部分功能需要客户端也安装本模组才能生效，详见下方说明。

## ✨ 主要功能

### 🤖 假人增强
- **假人驻留**：服务器重启后自动召唤已记录的假人。
- **假人动作保存**：每30秒记录假人的持续动作，重启后恢复。
- **假人自动打包**：假人可自动将背包内物品打包成潜影盒，支持配置打包种类数。

### ⚡ 性能优化
- **实体堆叠渲染优化**：只渲染部分大量同位置同类型实体，显著提高 FPS。
- **无碰撞堆叠实体 AI 优化**：脚手架/藤蔓等环境下的堆叠实体仅保留一个运算 AI，降低 MSPT(还需打磨)。

### 📦 实用工具
- **个人高亮** (`/eun highlight`)：为指定玩家高亮周围的物品或生物。
- **珍珠炮编码工具** (`/eun pearlCannon`)：二进制编码计算，支持自定义方案。
- **全局计分板**：轮播显示玩家挖掘、击杀、飞行距离等统计数据排名。
- **可合成隐形展示框**：8 个展示框 + 1 个紫水晶合成 8 个隐形展示框。
- **自动装备图腾**：即将死亡时自动从背包/容器装备不死图腾。
- **禁止下界荒地生成岩浆怪**：仅禁止自然生成，刷怪笼不受影响。

---

## 📥 安装

### 前置模组
- [Fabric Loader](https://fabricmc.net/use/) (≥0.18.4)
- [Fabric API](https://modrinth.com/mod/fabric-api) (≥0.141.3+1.21.11)
- [Carpet Mod](https://github.com/gnembon/fabric-carpet) (≥1.4.194)

### 安装步骤
1. 下载本模组的 `.jar` 文件。
2. 放入 Minecraft 游戏目录下的 `mods` 文件夹。
3. 启动游戏（确保已安装前置模组）。

---

## ⚙️ 配置

模组的详细参数通过配置文件管理，位于 `config/eun_carpet/` 目录下。

| 文件 | 说明 |
|------|------|
| `entity_optimization.json` | 实体渲染优化参数（最小堆叠数、保留公式、平滑因子等） |
| `ai_optimization.json` | AI 优化参数（启用实体类型、检测间隔、移动阈值等） |
| `packet/configs.json` | 假人自动打包配置（自动生成，无需手动编辑） |
| `pearlCannon/*.json` | 珍珠炮编码方案（可自定义添加） |

所有规则开关通过 Carpet 规则系统控制（`/carpet` 命令或 `carpet.conf`）。

### 重载配置
修改配置文件后，可使用命令 `/eun reload` 热重载，无需重启服务器。

---

## 🎮 命令

### `/eun highlight`
为玩家开启/关闭物品或生物高亮。
/eun highlight <item|entity> <玩家> <颜色> <true|false>
- 颜色：支持原版 16 种颜色名称（如 `red`、`blue`、`green`）。

### `/eun pearlCannon`
珍珠炮二进制编码计算。
/eun pearlCannon set <方案名> <参数...>
- 方案文件位于 `config/eun_carpet/pearlCannon/`。
- 示例：`/eun pearlCannon set default 100 east enable 50`

### `/eun packet`
假人自动打包管理。
- 方案文件位于 `config/eun_carpet/pearlCannon/`。
- 示例：`/eun pearlCannon set default 100 east enable 50`

### `/eun packet`
假人自动打包管理。
/eun packet set <假人名> <种类数(1-5)> <true|false>
/eun packet list
/eun packet info <假人名>
/eun packet remove <假人名>

### `/eun reload`
重载所有配置文件（需要 OP 等级 2 或控制台执行）。

---

## 🔧 Carpet 规则

| 规则名 | 默认值 | 说明                    |
|--------|--------|-----------------------|
| `fakePlayerPersistence` | `true` | 假人驻留                  |
| `fakePlayerActionSaving` | `false` | 假人动作保存                |
| `fakePlayerPrefix` | `false` | 假人前缀                  |
| `entityOptimizationEnabled` | `true` | 实体渲染优化总开关             |
| `pearlCannonEnabled` | `true` | 珍珠炮工具（支持权限等级）         |
| `globalScoreboardEnabled` | `false` | 全局计分板                 |
| `highlightEnabled` | `true` | 个人高亮（支持权限等级）          |
| `aiOptimizationEnabled` | `false` | AI 优化总开关(还需打磨,效果不太理想) |
| `craftableInvisibleItemFrames` | `false` | 可合成隐形展示框              |
| `autoTotemEquip` | `false` | 自动装备图腾                |
| `suppressMagmaCubeInNetherWastes` | `false` | 禁止下界荒地生成岩浆怪           |
| `packetEnabled` | `true` | 假人自动打包（支持权限等级）        |

规则值 `"true"`/`"false"` 或 `"0"`-`"4"` 表示所需 OP 等级（`"0"` 表示所有玩家可用）。

---

📄 许可
本项目采用 MIT License。

🙏 鸣谢
Fabric Carpet 团队提供的扩展框架。

所有为该项目提供建议和测试的玩家。

## 🛠️ 开发与构建

### 环境要求
- JDK 21+
- Gradle 9.2.1+

### 构建步骤
```bash
git clone https://github.com/zhang132212/eun-carpet-addition.git
cd eun-carpet-addition
./gradlew build

