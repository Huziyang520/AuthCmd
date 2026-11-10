# Changelog

## 1.5.0

- The config engine and the config screens now live in the shared library **Camelot**, so every mod on this project uses the same implementation and the same look.
  配置引擎与配置界面迁移到共享库 **Camelot**，本项目所有模组共用同一套实现与外观。
- Configuration is split into `*-common.toml` (rules, synced from the server) and `*-client.toml` (local preferences only).
  配置拆分为 `*-common.toml`（规则，由服务端同步）与 `*-client.toml`（仅本机偏好）。
- New `enable_config_entry` option (replaces the old pause-button switch): when off, the mod-list config entry and the pause-menu button are both unavailable.
  新增 `enable_config_entry`（取代原来的暂停按钮开关）：关闭后模组列表配置入口与暂停菜单按钮同时不可用。
- The pause-menu button opens the AvalonBase editor only; the mod-list entry is the way into the mod's own screen.
  暂停菜单按钮只打开 AvalonBase 编辑器；模组自身的配置界面走模组列表入口。
- Mode is now an enum selector; screen switches are queued so opening and closing no longer flashes.
  模式改为枚举下拉选择；界面切换改为排队处理，打开/关闭不再闪烁。
- Per-target loader metadata: the NeoForge version range is declared for each target, and the 26.1-26.2 target builds against 26.1.2.
  按区间声明加载器元数据：每个 target 单独声明 NeoForge 版本区间，26.1-26.2 目标基于 26.1.2 构建。
 — AuthCmd

## 1.4.3

- Added the "Allow entity selectors" toggle (default on). Non-OP players in restricted modes can now autocomplete and use `@` target selectors; when disabled, the server rejects any command containing one and shows a notice.
  新增「允许目标选择器」开关（默认开启）。受控模式下的非 OP 玩家也能正常补全并使用 `@` 目标选择器；关闭后服务端会拒绝任何含目标选择器的指令并给出提示。
- Fixed: with the non-OP whitelist active and `gamemode` not listed, non-OP players could still open the F3+F4 game mode switcher. It now behaves like vanilla without the mod (permission error, no UI); the switcher unlocks only when `gamemode` is listed.
  修复：非 OP 白名单模式下名单未写入 `gamemode` 时，非 OP 玩家仍能打开 F3+F4 游戏模式切换器。现在与未安装本模组时一致（提示无权限、不打开界面），仅当名单写入 `gamemode` 后可用。
- Added the "Enable animations" toggle (default on). Open/close animations for the config screen and the "Add" dialog.
  新增「启用动画效果」开关（默认开启）。配置界面与「新增」弹窗的打开/关闭动画。
- Renamed the "Interface settings" card to "Settings", and aligned the checkbox and toggle columns.
  「界面设置」卡片改名为「设置」，并统一勾选框列与开关列的对齐。
- Fixed the read-only overlay scaling with the open/close animation (it no longer moves or zooms).
  修复只读遮罩随开/关屏动画缩放的问题（不再位移或缩放）。
- Moved the permission notice onto the same line as the Save/Cancel buttons.
  权限提示移到与「保存/取消」按钮同一行。
- Added a clickable "Backup feedback address" link on the Mod Menu page.
  新增 Mod Menu 页面可点击的「备用反馈地址」链接。
- README now documents the optional library AvalonBase with a download link.
  README 补充可选前置库 AvalonBase 的说明与下载链接。
