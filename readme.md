## meshchat 去中心化的聊天软件

### 多语言

界面文案已抽离到字符串资源，随系统语言切换：

- **默认 / 回退**：`res/values/strings.xml`（简体中文，与 `zh-rCN` 一致）
- **English**：`res/values-en/strings.xml`
- **简体中文**：`res/values-zh-rCN/strings.xml`
- **繁体中文（台湾）**：`res/values-zh-rTW/strings.xml`

在系统设置中切换显示语言后，应用会加载对应资源（可能需要重启应用或重建 Activity）。
