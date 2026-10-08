**English** | [中文](#中文)

---

# AuthCmd

> A command-permission controller for Minecraft servers: decide exactly which commands each player is allowed to use.

## What it does

AuthCmd gives server owners two independent rule sets plus one shared behaviour:

| Rule set | Applies to | Meaning |
|---|---|---|
| **Non-OP whitelist** | regular players | The commands you list become **usable by regular players** (they are normally operator-only). Everything else keeps its vanilla behaviour. |
| **OP blacklist** | operators | The commands you list become **blocked for operators**. Everything else stays available. |
| **Game mode switching** | both | `/gamemode`, `/g` and the F3 + F4 game mode switcher follow the `gamemode` entry of the rule set that applies to the player. Add `gamemode` to the list to allow it, leave it out to block it. |

Each rule set has its **own exemption list**: players on it are completely ignored by that rule set.

Four modes are available: **disabled**, **non-OP whitelist only**, **OP blacklist only**, or **both**.

## Quick start

1. Put the mod file into your server's `mods` folder (Fabric or NeoForge).
2. Start the server once. The config file `config/authcmd.toml` is created automatically.
3. Either edit that file, or use the in-game editor (see below).
4. Pick a mode, fill in the command list and the exemption list, save.

## The in-game editor

- Open it from the **pause menu** (the mod's edit button) or from the **mod list** → *AuthCmd* → *Config*.
- The visual editor needs the optional library **AvalonBase** installed on the client, plus operator permission on the server. Without AvalonBase the mod still works — you just edit the config file by hand.
  - AvalonBase: <https://www.curseforge.com/minecraft/mc-mods/avalonbase>
- If the server owner turned the edit entry off, the mod list entry still opens, but only shows a short "entry closed" notice.
- The editor's *Interface* section contains a **Show pause button** switch and an **Enable interface animations** switch (both on by default).

## Chat messages

Blocked commands answer with a red `AuthCmd: This command has been disabled!`. Blocked game mode switching answers with `AuthCmd:`-style red text about game mode switching.

## Config file

`config/authcmd.toml` — the file is plain TOML and can be edited by hand at any time; changes are picked up **without restarting the server**.

| Key | Meaning |
|---|---|
| `mode` | `disabled` / `non_op_only` / `op_only` / `both` |
| `show_pause_button` | Show the edit button in the pause menu |
| `enable_animations` | Interface open/close animations |
| `non_op_whitelist` / `op_blacklist` | The command lists |
| `non_op_exempt` / `op_exempt` | The exemption lists |

## Good to know

- The non-OP whitelist **only grants extra permissions** — it never takes commands away from regular players.
- Command names are matched case-insensitively; a leading `/` is ignored.
- `/gamemode` and `/g` are treated as the same command.
- Everything is decided on the **server**; installing the mod on the client only adds the visual editor.

## Links

- Project page: <https://www.curseforge.com/minecraft/mc-mods/authcmd>
- Feedback (backup): <https://issue.mengcai.online/>

## License

MIT — author: Huziyang520

---
---

<a id="中文"></a>

[English](#authcmd) | **中文**

---

# AuthCmd

> Minecraft 服务端的指令权限控制器：精确决定"哪个玩家能用哪些指令"。

## 它做什么

AuthCmd 给服主提供两套互相独立的规则，外加一个共用行为：

| 规则 | 作用对象 | 含义 |
|---|---|---|
| **非OP 玩家指令白名单** | 普通玩家 | 名单里写的指令，**普通玩家也能使用**（这些指令原本只有OP能用）。名单外的指令保持原版行为不变。 |
| **OP 玩家指令黑名单** | 管理员（OP） | 名单里写的指令，**管理员也不能使用**。名单外的指令照常可用。 |
| **游戏模式切换** | 两者 | `/gamemode`、`/g` 与 F3+F4 游戏模式切换器，都跟随该玩家所处规则里的 `gamemode` 条目：名单里写了就能用，没写就别用。 |

每套规则还各有**一份豁免名单**：在豁免名单里的玩家完全不受该规则约束。

共 4 种模式：**全关闭**、**仅非OP白名单**、**仅OP黑名单**、**同时启用**。

## 快速开始

1. 把模组文件放进服务端的 `mods` 文件夹（Fabric 或 NeoForge）。
2. 启动一次服务端，会自动生成配置文件 `config/authcmd.toml`。
3. 直接改这个文件，或者用游戏内编辑界面（见下）。
4. 选好模式、填好指令名单与豁免名单，保存即可。

## 游戏内编辑界面

- 从**暂停菜单**的编辑按钮进入，或从**模组列表** → *AuthCmd* → *配置* 进入。
- 可视化编辑界面需要客户端安装可选前置库 **AvalonBase**，并且在服务端拥有管理员权限；不装 AvalonBase 也能正常使用，只是只能手改配置文件。
  - AvalonBase 下载：<https://www.curseforge.com/minecraft/mc-mods/avalonbase>
- 如果服主关闭了编辑入口，模组列表入口仍会打开，但只显示一句"服务端关闭了编辑页面入口"的提示。
- 界面的「界面设置」里有 **显示暂停按钮** 与 **启用动画效果** 两个开关（默认都开启）。

## 聊天提示

被拦截的指令会收到红字 `AuthCmd: 该指令已被禁止！`；被拦截的游戏模式切换则显示对应的游戏模式切换提示。

## 配置文件

`config/authcmd.toml` —— 纯 TOML 文本，随时可以手改，**改动无需重启服务端**即可生效。

| 键 | 含义 |
|---|---|
| `mode` | `disabled` / `non_op_only` / `op_only` / `both` |
| `show_pause_button` | 是否显示暂停菜单里的编辑按钮 |
| `enable_animations` | 是否启用界面开/关动画 |
| `non_op_whitelist` / `op_blacklist` | 指令名单 |
| `non_op_exempt` / `op_exempt` | 豁免玩家名单 |

## 使用须知

- 非OP 白名单**只做"额外放行"**，不会夺走普通玩家原本就能用的指令。
- 指令名不区分大小写，开头的 `/` 会被忽略。
- `/gamemode` 与 `/g` 视为同一条指令。
- 所有判定都在**服务端**完成；客户端装本模组只是为了获得可视化编辑界面。

## 相关链接

- 项目主页：<https://www.curseforge.com/minecraft/mc-mods/authcmd>
- 备用反馈地址：<https://issue.mengcai.online/>

## 许可证

MIT —— 作者：Huziyang520
