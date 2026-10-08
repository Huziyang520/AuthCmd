# Changelog — AuthCmd

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
