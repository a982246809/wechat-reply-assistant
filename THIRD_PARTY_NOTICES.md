# 第三方软件声明

《AI 微信回复助手》使用第三方开源软件。第三方组件分别适用其自身的许可证；本项目不会改变这些许可证，也不会将第三方组件重新授权为 Apache License 2.0。

以下为项目使用的主要组件类别，实际版本和传递依赖以构建时 Gradle 解析结果为准。许可证摘要不能替代各组件随附的完整许可证文本。

| 组件类别 | 项目中的示例模块 | 主要许可证 |
| --- | --- | --- |
| AndroidX / Jetpack | Core KTX、AppCompat、Activity、Lifecycle、DataStore、ConstraintLayout、CardView | Apache License 2.0 |
| Jetpack Compose | Compose BOM、UI、Material 3、Tooling | Apache License 2.0 |
| Hilt / Dagger | Hilt Android、Hilt Compiler、Hilt Navigation Compose | Apache License 2.0 |
| Kotlin | Kotlin 标准库与编译器插件、kotlinx.coroutines、kotlinx.serialization | Apache License 2.0 |
| 网络与序列化 | OkHttp、MockWebServer、Retrofit、kotlinx.serialization converter | Apache License 2.0 |
| Material Components | Material Components for Android | Apache License 2.0 |
| 单元测试 | JUnit 4 | Eclipse Public License 1.0 |
| 单元测试辅助 | MockK | Apache License 2.0 |
| 单元测试辅助 | Robolectric | MIT License |
| Android 测试 | AndroidX Test、Espresso、Compose UI Test | Apache License 2.0 |

## 版本与构建时依赖

本文件不固定依赖版本，也不替代 Gradle 生成的依赖图。当前版本由项目的版本目录、插件配置和 Gradle 解析结果决定；构建时请以实际 Gradle 依赖为准，并检查对应 artifact 或上游仓库附带的许可证与 NOTICE。

```bash
./gradlew :app:dependencies --configuration debugRuntimeClasspath
```

## 分发注意事项

如果你分发本项目的源码或二进制包，应：

- 保留本项目根目录的 `LICENSE` 和 `NOTICE`。
- 随分发物提供第三方许可证和归属声明，或按照各许可证要求的其他方式提供。
- 不要删除第三方组件中的版权、专利、商标或归属通知。
- 在替换、升级或新增依赖后重新核对许可证兼容性，并更新本文件。

本文件只作项目内记录，不构成法律意见。对具体许可条款有疑问时，请咨询相应组件的上游项目或专业法律顾问。
