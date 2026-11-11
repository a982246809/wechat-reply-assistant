# 贡献指南

感谢你帮助改进《AI 微信回复助手》。提交代码、测试、文档或问题报告前，请先阅读本指南和 [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)。

## 基本要求

- 使用 JDK 21。
- 保持改动聚焦，不把无关的重构、格式化或依赖升级混入同一个 Pull Request。
- 新增行为应补充测试；修复缺陷时优先加入能复现问题的测试。
- 涉及 Provider、权限、备份或本地存储的改动，应同步更新 [PRIVACY.md](PRIVACY.md)、[SECURITY.md](SECURITY.md) 或相关文档。
- 新增或升级第三方依赖时，确认许可证兼容性，并更新 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

## 隐私与测试数据

**禁止提交真实的聊天记录、联系人信息、头像、昵称、手机号、二维码、聊天截图、API Key、访问令牌或生产日志。**

请使用合成的测试数据：

- 使用虚构的联系人、消息和场景；
- 使用 `example.com`、`example.test` 或本地 mock server，不使用真实 Provider 地址；
- 使用明显无效的占位 API Key，例如 `sk-REPLACE_ME`；
- 为 UI 截图使用合成数据或专门制作的测试画面；
- 对必要的错误日志做脱敏，并删除请求头、Cookie、密钥和个人标识。

如果问题只有真实聊天才能复现，请将其缩小为结构相同的最小 mock，或在私密安全报告中说明，不要把原始数据贴到公开 issue。

## 开发与验证

在项目根目录运行：

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

提交前应运行与 CI 相同的完整命令：

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Debug APK 生成在 `app/build/outputs/apk/debug/`。如果本地修改了 Gradle 配置、资源或 Provider 请求格式，请在 Pull Request 中说明影响范围，并附上脱敏后的验证结果。

## 提交 Pull Request

1. 先创建或关联一个 issue，说明问题、使用场景和预期行为。
2. 保持提交信息清晰，说明“做了什么”和“为什么”。
3. 在 PR 模板中填写变更摘要、验证命令和隐私确认。
4. 确认没有提交构建产物、签名文件、本地 SDK 路径或任何凭据。
5. 修改 UI 时使用合成数据截图；不要使用真实联系人或真实聊天。

维护者可能会要求拆分过大的改动，或先补充测试和文档。

## 安全漏洞

不要在公开 issue 或 Pull Request 中报告安全漏洞。请按照 [SECURITY.md](SECURITY.md) 的私密渠道提交，并避免附带真实聊天、截图、API Key 或二维码。

## 许可证

除非你明确另行说明，向本项目提交的贡献均按 [Apache License 2.0](LICENSE) 授权。提交即表示你有权按该许可证提供相关内容，并且没有引入与项目许可证不兼容的第三方代码或素材。
