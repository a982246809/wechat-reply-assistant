# AI 微信回复助手

[![Android CI](https://github.com/a982246809/wechat-reply-assistant/actions/workflows/android-ci.yml/badge.svg)](https://github.com/a982246809/wechat-reply-assistant/actions/workflows/android-ci.yml)
[![GitHub release](https://img.shields.io/github/v/release/a982246809/wechat-reply-assistant)](https://github.com/a982246809/wechat-reply-assistant/releases/latest)
[![License: Apache-2.0](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

《AI 微信回复助手》是一款 Android 端的微信回复建议工具。应用根据当前聊天内容生成文本或视觉回复建议，并通过用户自行配置的 OpenAI-compatible Provider 完成推理。

项目采用“自带 Provider”的方式：应用不提供业务服务端、不中转请求，也不要求注册账号。聊天内容只会直接发送到用户填写的 Provider 地址。

仓库：<https://github.com/a982246809/wechat-reply-assistant> · 下载：<https://github.com/a982246809/wechat-reply-assistant/releases>

> 本项目与腾讯、微信及其关联公司无任何关联，未获得其认可、赞助或授权。相关名称和商标的使用说明见 [TRADEMARKS.md](TRADEMARKS.md)。

## 功能范围

首版聚焦于本地回复辅助，包含：

- Android 应用（`applicationId`：`io.github.a982246809.replyassistant`）
- OpenAI-compatible Provider 配置：Base URL、API Key、主模型和可选视觉模型
- 通过 `GET {baseUrl}/models` 查询模型，通过 `POST {baseUrl}/chat/completions` 生成回复
- 文本回复建议，以及使用视觉模型处理图片场景（视觉截图读取需要 Android 14+）
- 微信界面上的悬浮球
- 自动触发建议和手动触发建议
- 排除指定单聊，避免在这些聊天中触发建议
- 配置每次生成的回复建议数量
- 本地保存设置；API Key 使用 Android Keystore 加密保存，并排除在系统备份之外

## 非目标

首版明确不包含以下能力：

- 账号注册、登录、额度、会员、邀请、支付或应用内客服系统
- 聊天记忆或跨会话长期上下文
- 开发者工具
- 应用内更新
- 遥测、行为分析或广告 SDK
- 由本项目运营的云端消息中转、代理或存储服务
- iOS 版本或其他即时通讯平台的适配

## Quick Start (English)

### Prerequisites

- A physical or emulated Android device (the current app supports Android API 24+; visual screenshot capture requires Android 14+).
- WeChat installed on the device.
- An OpenAI-compatible Provider endpoint, an API key, and at least one chat model.
- Android SDK, JDK 21, and network access for Gradle dependencies when building from source.

### Configure and run

1. Build and install the debug app:

   ```bash
   ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
   ```

2. Open the app and configure the Provider:

   - Base URL: `https://api.example.com/v1`
   - API Key: use a provider key that you are authorized to use
   - Main model: `example-chat-model`
   - Vision model: `example-vision-model` (optional)

3. Enable the accessibility service for the app. The floating ball is shown through the accessibility overlay while the service is active; allow other Android permissions if the current build prompts for notifications or background work.

4. Open a one-to-one WeChat chat and trigger a suggestion from the floating ball, or enable automatic suggestions.

5. Start with synthetic test conversations. Do not connect a real account until you have reviewed [PRIVACY.md](PRIVACY.md) and the HTTP risk below.

## Provider 配置

### 配置字段

| 字段 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- |
| Base URL | 是 | `https://api.example.com/v1` | Provider 的 API 根地址；应用会在此基础上拼接 `/models` 和 `/chat/completions` |
| API Key | 通常必填 | `sk-REPLACE_ME` | Provider 需要鉴权时必填；本机无鉴权服务可留空。仅持久化在本机并使用 Android Keystore 加密，请求时作为 Bearer 头发送给所配置的 Provider；不要提交到仓库或粘贴到公开 issue |
| 主模型 | 是 | `example-chat-model` | 用于文本回复的模型 ID |
| 视觉模型 | 否 | `example-vision-model` | 需要分析图片时使用的模型 ID；留空时使用主模型处理视觉请求，主模型不支持图片时请求会失败 |

Base URL 推荐填写到 API 根路径，例如 `https://api.example.com/v1`。如果误填了以 `/models` 或 `/chat/completions` 结尾的完整端点，应用会自动归一化。不同 Provider 对路径、鉴权头和模型字段可能有差异，请以 Provider 文档为准。

### 端点格式

```text
GET  {baseUrl}/models
POST {baseUrl}/chat/completions
```

使用上面的示例配置后，请求地址为：

```text
GET  https://api.example.com/v1/models
POST https://api.example.com/v1/chat/completions
```

客户端按 OpenAI-compatible 约定使用 Bearer API Key：

```http
GET /v1/models HTTP/1.1
Host: api.example.com
Authorization: Bearer sk-REPLACE_ME
```

```http
POST /v1/chat/completions HTTP/1.1
Host: api.example.com
Authorization: Bearer sk-REPLACE_ME
Content-Type: application/json

{
  "model": "example-chat-model",
  "messages": [
    {
      "role": "user",
      "content": "请根据当前聊天生成一条简短回复。"
    }
  ]
}
```

以上示例只说明配置格式，不包含可用的真实密钥。Provider 返回的 `choices`、模型列表和错误格式需与 OpenAI-compatible API 兼容；具体字段支持情况由 Provider 决定。

## 构建要求

- JDK 21
- Android SDK（安装构建所需的平台和构建工具；以 Gradle 配置和 Android Studio 提示为准）
- Android Studio 或可用的 Android SDK 命令行环境
- 可访问 Maven 仓库的网络连接

在项目根目录运行完整验证：

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Debug APK 生成在 `app/build/outputs/apk/debug/`。项目不提供 release 签名或发布配置；CI 只构建 debug APK，不读取签名密钥，也不执行发布。

如本机尚未设置 Java 21，可将 `JAVA_HOME` 指向 JDK 21 后重新运行 Gradle。Windows 用户可使用 `gradlew.bat`。

## HTTP 风险

应用允许使用 `http://` 地址并会提示明文传输风险，但强烈建议使用 `https://`。HTTP 模式下，以下内容可能被同一 Wi-Fi、热点、代理或网络设备读取或篡改：

- API Key
- 聊天内容和图片
- 模型返回的回复
- Provider 返回的错误信息

不要在公共 Wi-Fi、不可信热点或不受控代理下使用 HTTP。即使使用 HTTPS，也应确认设备时间、证书链和 Provider 地址正确。

## 隐私与安全边界

- 数据只发送给用户自行配置的 Provider；本项目不设中转服务端，也不代表该 Provider 的隐私实践。
- 应用不将聊天原文持久化到本地存储；发送给 Provider 的内容仍受 Provider 的日志、保留和训练政策约束。
- 排除的聊天标题和普通配置保存在本机。
- API Key 使用 Android Keystore 加密保存，并明确排除在系统备份之外。
- 应用不包含账号体系或内置遥测；首版不收集使用统计。
- 使用带图/视觉建议时，相关画面会直接发送到用户配置的 Provider；未单独填写视觉模型时可能使用主模型。
- HTTP 可能明文传输聊天内容和密钥；HTTPS 是推荐配置。

完整说明见 [PRIVACY.md](PRIVACY.md)。安全问题请按 [SECURITY.md](SECURITY.md) 私密报告，不要在公开 issue 中披露漏洞、真实聊天、截图、API Key 或二维码。

## 参与贡献

欢迎提交缺陷修复、测试和文档改进。开始前请阅读 [CONTRIBUTING.md](CONTRIBUTING.md) 和 [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)。

贡献者必须使用合成的测试数据和脱敏日志，禁止提交真实聊天、联系人信息、截图、二维码、API Key 或其他凭据。提交前请运行上面的完整验证命令。

安全漏洞请使用仓库的私密安全报告渠道；不要创建公开 issue。第三方依赖和许可证说明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

## 许可证

本项目采用 Apache-2.0 许可证，全文见 [Apache License 2.0](LICENSE)。

Copyright 2026 The wechat-reply-assistant contributors

版权归属和第三方归属分别见 [NOTICE](NOTICE) 与 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。Apache 2.0 许可证不授予项目名称、微信/WeChat 名称或任何第三方商标的使用许可，详见 [TRADEMARKS.md](TRADEMARKS.md)。
