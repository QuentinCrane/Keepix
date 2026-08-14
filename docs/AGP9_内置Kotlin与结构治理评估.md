# AGP 9、内置 Kotlin 与结构治理评估

> 评估日期：2026-08-14
>
> 本记录对应实施规划 F06。它是迁移前的审计与决策记录，不代表已经完成 AGP 9 迁移。

## 1. 当前基线

本次从仓库实际配置核对出以下基线：

| 项目 | 当前值 | 结论 |
| --- | --- | --- |
| Android Gradle Plugin | `8.13.2` | 继续作为 API 36 发布线 |
| Gradle Wrapper | `8.13` | 与当前 AGP 配套，尚未进入 AGP 9 所需的 Gradle 9.x 线 |
| JDK | 17 | 满足当前配置，也满足 AGP 9 的 JDK 基线 |
| Kotlin Gradle Plugin | `2.2.21` | 与当前 Room/Hilt/kapt 组合已验证 |
| Compose 插件 | `org.jetbrains.kotlin.plugin.compose` `2.2.21` | 继续与 Kotlin 版本保持一致 |
| 注解处理 | `org.jetbrains.kotlin.kapt` | 仓库约束明确要求保留 kapt，未批准迁移 KSP |
| Room / Hilt | Room `2.8.4`、Hilt `2.56.1` | 本次 AndroidX 依赖升级门禁已通过 |
| 模块 | `:app`、`:benchmark` | 暂无可度量的模块拆分收益 |
| Android API | `compileSdk = 36`、`targetSdk = 36`、`minSdk = 30` | API 36 发布合规基线已建立 |

## 2. AGP 9 兼容性审计

官方迁移说明显示，AGP 9 默认启用内置 Kotlin，并要求移除 `org.jetbrains.kotlin.android`；内置 Kotlin 与 `org.jetbrains.kotlin.kapt` 不兼容。能够迁移到 KSP 的处理器应逐项迁移，不能迁移的处理器才使用 AGP 提供的 `com.android.legacy-kapt` 过渡方案。参考：

- [AGP 9.0 release notes](https://developer.android.com/build/releases/agp-9-0-0-release-notes)
- [Migrate to built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin)

逐项检查结果：

| 检查项 | 结果 | 迁移前动作 |
| --- | --- | --- |
| Gradle / AGP / JDK | JDK 17 已满足；Wrapper 和 AGP 仍在 8.x | 使用 Android Studio Upgrade Assistant 统一升级，不手工拼接半套版本 |
| `kotlin-android` | 根插件和两个 Android 模块仍使用 `org.jetbrains.kotlin.android` | AGP 9 迁移时按官方向导移除，并重新确认模块 Kotlin DSL |
| Compose Kotlin 插件 | 当前单独应用 Compose 编译器插件 | 升级后确认内置 Kotlin 与 Compose 插件的声明方式及版本约束 |
| `kapt` | Room、Hilt、AndroidTest Hilt 均依赖 kapt | 默认不迁移 KSP；若继续使用 kapt，迁移时评估 `com.android.legacy-kapt`，并单独验证代码生成 |
| Hilt / Room 生成 | 当前版本在 Kotlin 2.2.21 + AGP 8.13.2 下通过 | AGP 9 前单独核对 Hilt/Dagger、Room compiler 的兼容版本；不可与功能改写混合 |
| 旧 Android Variant API | 未发现 `BaseExtension`、`applicationVariants`、`variantFilter`、`com.android.build.gradle` 等调用 | 保持现状；升级后用 `help`、编译和 R8 复查插件行为 |
| 自定义 BuildConfig | 未发现自定义 `buildConfigField`，只使用标准 `BuildConfig` 字段 | 保留 `buildFeatures.buildConfig = true`，不新增生成字段 |
| 自定义 source set / SDK 路径 | 未发现自定义 source set、`sdkDirectory`、`adbExecutable` 或 boot classpath 逻辑 | 不需要预先改造 |

## 3. 结构治理结论

当前只有一个业务模块 `:app`，`:benchmark` 只承载基准测试。代码虽然由单一 ViewModel 统筹多个页面，但当前规划已经通过接口、契约测试和局部工具类形成了可追踪边界。现阶段没有构建时间、增量编译、团队所有权或发布隔离数据证明拆分模块值得承担额外成本。

因此本阶段的结构决策是：

1. 保持 `:app` 单业务模块。
2. 继续在 `:app` 内按 Repository、MediaStore、Worker、设置和 UI 契约逐领域拆类。
3. 只有在记录到可重复的构建时间、增量编译或所有权收益后，才另立模块化提案。
4. AGP 9、KSP、模块化和功能改写禁止放入同一个提交。

## 4. 本阶段决策

F06 作为“迁移评估”完成，实际 AGP 9 迁移延期，原因是：

- 当前发布线仍以 AGP 8.13.2 + API 36 为低风险稳定基线；
- 官方建议使用 Android Studio Upgrade Assistant 执行 AGP 9 迁移；
- 内置 Kotlin 会改变 `org.jetbrains.kotlin.android` 与 `kapt` 的处理方式，而仓库尚未批准 KSP，也没有必要在发布闭环中引入该风险；
- 迁移需要单独处理 Gradle Wrapper、Hilt/Dagger、Room、Compose 插件和生成代码门禁。

本记录提交不修改构建配置，不声称 AGP 9、KSP、真机运行或模块化已经完成。

## 5. 未来单独迁移块的验收门

获得迁移授权后，必须以独立提交开始，并保存迁移前后证据：

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:help --configuration-cache --console=plain --no-daemon
.\gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleRelease --configuration-cache --console=plain --no-daemon --max-workers=1
```

同时需要：

- 在干净克隆中重复上述门禁；
- 对比迁移前后的配置缓存命中、首次构建和增量构建时间；
- 核对 Room/Hilt 生成任务没有消失、跳过或产生空输出；
- 核对 Release R8、APK 元数据和安装启动；
- 单独记录失败时的回退点，不把未验证的 APK 或“控制台出现 Built”当作成功。
