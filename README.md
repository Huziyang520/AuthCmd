# AuthCmd

> 一个基于可配置权限名单，限制玩家**能执行的指令**与**能切换的游戏模式**的 Minecraft 1.20.1 模组。

## 基本信息

| 项 | 值 |
|---|---|
| 名称 | AuthCmd |
| Mod ID | `authcmd` |
| 版本 | `1.0.0` |
| 支持平台 | Forge `47.2.x` / Fabric Loader `0.16.9` / NeoForge 1.20.1 |
| 前置 | AvalonBase（可选，仅用于暂停页面的可视化编辑界面） |
| 作者 | Huziyang520 |
| 开源协议 | MIT |
| 反馈 | [GitHub Issues](https://github.com/Huziyang520/AuthCmd/issues) / [issue.mengcai.online](https://issue.mengcai.online/) |

## 模组功能

AuthCmd 提供**两套独立的指令/游戏模式权限控制**，按玩家是否为 OP 分别生效：

### 功能一：非OP玩家指令白名单
- 对**非OP玩家**生效。
- 白名单（`non_op_whitelist`）内的指令被**提权放行**：即使原本需要权限2才能使用，非OP也能正常执行；白名单外的指令**不做拦截**，交还原版处理（非OP原本能用则照常用，原本不能用则由原版拦截）。
- **支持 Tab 补全**：白名单内的指令对非OP玩家也能正常补全（命令树按白名单动态放行）。
- 白名单条目支持 `gamemode` 这种根命令，命中后其子命令一并放行。


### 功能二：OP玩家指令黑名单
- 对 **OP玩家**生效。
- 黑名单（`op_blacklist`）内的指令被拦截，其余可正常使用。

### 游戏模式限制
- 可单独限制 OP / 非OP 玩家能否切换游戏模式（`gamemode` 命令）。
- 支持按玩家/命令细粒度豁免。

### 豁免玩家
- 在豁免名单（`non_op_exempt` / `op_exempt`）中的玩家**完全放行**，不受上述限制。

### 四种生效模式
`mode` 配置可选：
- `disabled`：全部关闭（模组不干预）
- `non_op_only`：仅功能一（非OP白名单）生效
- `op_only`：仅功能二（OP黑名单）生效
- `both`：功能一、功能二同时生效

## 配置说明

配置文件：`config/authcmd.toml`，包含：
- `mode`：生效模式
- `show_pause_button`：是否显示暂停页面编辑按钮
- `show_tips`：进入世界时（未装 AvalonBase）是否显示可视化编辑提示
- `non_op_whitelist`：非OP指令白名单
- `op_blacklist`：OP指令黑名单
- `non_op_exempt` / `op_exempt`：豁免玩家名单

配置变更后，会**自动向所有在线玩家重发命令树**，客户端补全即时更新。

## 与 AvalonBase 的关系（可选联动）

- **未安装 AvalonBase**：AuthCmd 作为纯服务端模组独立运行，完全靠 `config/authcmd.toml` 驱动核心功能（指令限制、游戏模式限制、Tab 补全），一切正常。
- **安装 AvalonBase**：额外获得暂停页面上的**可视化编辑按钮**，可在游戏内直接编辑上述名单（无需手动改配置）。
- 若未装 AvalonBase 且配置允许，进入世界时会在聊天框提示"安装 AvalonBase 可启用可视化编辑"（可用 `show_tips` 关闭）。

## 技术信息

- 游戏版本：Minecraft 1.20.1
- Java 17
- 构建：MultiLoader（common / fabric / forge 三模块）
- 平台无关逻辑位于 common，网络层经 AvalonBase 的 `AvalonNetwork` 抽象（未装时通过反射探测自动跳过）
