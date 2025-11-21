# AdMob 插页式广告集成说明

## 概述

本文档说明如何完成 Google AdMob 插页式广告的集成和配置。

## 已完成的集成

### 1. Gradle 配置
- ✅ 已添加 `play-services-ads:24.4.0` 依赖
- ✅ 已添加 `user-messaging-platform:3.2.0` 依赖

### 2. AndroidManifest.xml 配置
- ✅ 已添加必要权限（INTERNET、ACCESS_NETWORK_STATE）
- ✅ 已添加 AdMob App ID 元数据占位符

### 3. 代码实现
- ✅ `GoogleMobileAdsConsentManager.kt` - UMP 同意流程管理
- ✅ `InterstitialAdManager.kt` - 插页式广告管理
- ✅ `NavActivity.kt` - MobileAds SDK 初始化和广告显示集成

## 需要替换的配置

### 1. AdMob App ID（AndroidManifest.xml）

**文件位置：** `app/src/main/AndroidManifest.xml`

**当前配置：**
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-3940256099942544~3347511713" />
```

**操作：**
- 将 `ca-app-pub-3940256099942544~3347511713` 替换为您的实际 AdMob App ID
- App ID 格式：`ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX`
- 获取方式：登录 AdMob 控制台 → 应用 → 查看应用设置

### 2. 插页式广告位 ID（InterstitialAdManager.kt）

**文件位置：** `app/src/main/java/com/example/compose/jetchat/ads/InterstitialAdManager.kt`

**当前配置：**
```kotlin
private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
```

**操作：**
- 将 `ca-app-pub-3940256099942544/1033173712` 替换为您的实际插页式广告位 ID
- 广告位 ID 格式：`ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX`
- 获取方式：登录 AdMob 控制台 → 应用 → 广告单元 → 创建/查看插页式广告单元

### 3. 测试设备 ID（NavActivity.kt）

**文件位置：** `app/src/main/java/com/example/compose/jetchat/NavActivity.kt`

**当前配置：**
```kotlin
val testDeviceIds = listOf(
    // "YOUR_TEST_DEVICE_ID_HERE" // 示例：从 logcat 中获取
)
```

**操作：**
1. 运行应用后，查看 logcat
2. 搜索包含 "Use RequestConfiguration.Builder().setTestDeviceIds" 的日志
3. 复制设备 ID 并添加到列表中

**示例：**
```kotlin
val testDeviceIds = listOf(
    "33BE2250B43518CCDA7DE426D04EE231" // 您的测试设备 ID
)
```

### 4. 同意流程调试设置（NavActivity.kt）

**文件位置：** `app/src/main/java/com/example/compose/jetchat/NavActivity.kt`

**当前配置：**
```kotlin
val isDebug = true // 仅用于测试，生产环境应设置为 false
```

**操作：**
- 测试阶段：保持 `isDebug = true`
- 生产环境：**必须**设置为 `isDebug = false`

## 假设条件

1. **应用包名：** `com.example.compose.jetchat.interstitial`（已在 build.gradle.kts 中配置）
2. **AdMob 账户：** 已创建并配置完成
3. **应用已注册：** 在 AdMob 控制台中已创建应用，包名与上述一致
4. **广告单元已创建：** 已创建插页式广告单元并获取广告位 ID
5. **网络连接：** 测试设备已连接互联网

## QA 验证步骤

### 1. 构建验证

```bash
# 从项目根目录运行
./gradlew assembleDebug

# 或构建特定变体
./gradlew assembleDemoDebug
```

**预期结果：**
- 构建成功，无错误
- APK 生成在 `app/build/outputs/apk/` 目录

### 2. 安装和运行

```bash
# 安装到设备
adb install app/build/outputs/apk/debug/app-debug.apk

# 或使用 Android Studio 直接运行
```

### 3. 测试设备配置验证

1. 运行应用
2. 查看 logcat，搜索 "MobileAds" 或 "InterstitialAdManager"
3. 确认看到以下日志：
   - "MobileAds SDK 版本: ..."
   - "插页式广告加载成功" 或相关错误信息
4. 如果看到测试设备 ID 提示，复制并添加到 `testDeviceIds` 列表

### 4. 同意流程验证

**测试模式（isDebug = true）：**
1. 应用启动后应显示同意表单（如果配置了测试地理位置）
2. 选择同意或拒绝，观察日志输出
3. 验证同意状态是否正确记录

**生产模式（isDebug = false）：**
1. 根据用户地理位置和法规要求显示同意表单
2. 验证同意流程正常工作

### 5. 广告加载验证

**查看 logcat 日志：**

**成功情况：**
```
InterstitialAdManager: 插页式广告加载成功
```

**失败情况（常见错误）：**
```
InterstitialAdManager: 插页式广告加载失败: code=0, message=...
```

**错误代码说明：**
- `ERROR_CODE_INTERNAL_ERROR (0)`: 内部错误
- `ERROR_CODE_INVALID_REQUEST (1)`: 无效请求（检查 App ID 和广告位 ID）
- `ERROR_CODE_NETWORK_ERROR (2)`: 网络错误
- `ERROR_CODE_NO_FILL (3)`: 无填充（正常，测试环境常见）

### 6. 广告显示验证

1. **触发点：** 点击侧边栏中的个人资料选项
2. **预期行为：**
   - 如果广告已加载：显示插页式广告，关闭后导航到个人资料页面
   - 如果广告未加载：直接导航到个人资料页面（正常流程）

3. **验证日志：**
   - "插页式广告已显示"
   - "广告已关闭"
   - "广告已点击"（如果用户点击了广告）

### 7. Ad Inspector 验证

**使用 Ad Inspector（推荐）：**

1. 在应用中长按广告（如果显示）
2. 选择 "Ad Inspector"
3. 验证广告信息：
   - 广告单元 ID
   - 请求配置
   - 网络响应

**或使用命令行：**

```bash
# 启用 Ad Inspector
adb shell setprop debug.google.ads.enable_inspector true

# 运行应用并查看广告
```

### 8. 测试广告验证

**当前使用的是 Google 测试广告位 ID：**
- App ID: `ca-app-pub-3940256099942544~3347511713`
- 广告位 ID: `ca-app-pub-3940256099942544/1033173712`

**测试广告特点：**
- 始终可用
- 不会产生收益
- 用于功能测试

**切换到真实广告：**
- 替换为您的实际 App ID 和广告位 ID
- 确保应用已在 AdMob 控制台中正确配置

## 常见问题排查

### 问题 1：广告未加载

**可能原因：**
- App ID 或广告位 ID 配置错误
- 网络连接问题
- 同意流程未完成
- 应用未在 AdMob 控制台中正确注册

**排查步骤：**
1. 检查 logcat 中的错误信息
2. 验证 App ID 和广告位 ID 格式
3. 确认网络连接正常
4. 检查同意流程是否完成

### 问题 2：同意表单未显示

**可能原因：**
- `isDebug = false` 且不在需要同意的地区
- 测试设备 ID 未配置
- UMP 配置问题

**排查步骤：**
1. 检查 `isDebug` 设置
2. 配置测试设备 ID
3. 查看 UMP 相关日志

### 问题 3：广告显示后应用崩溃

**可能原因：**
- Activity 生命周期问题
- 回调中执行了非法操作

**排查步骤：**
1. 查看崩溃日志
2. 检查 `onAdDismissed` 回调中的代码
3. 确保在正确的线程中执行 UI 操作

## 生产环境检查清单

- [ ] App ID 已替换为实际值
- [ ] 广告位 ID 已替换为实际值
- [ ] `isDebug` 已设置为 `false`
- [ ] 测试设备 ID 已移除或注释
- [ ] 应用已在 AdMob 控制台中正确配置
- [ ] 包名与 AdMob 中的应用包名一致
- [ ] 已测试同意流程
- [ ] 已测试广告加载和显示
- [ ] 已使用 Ad Inspector 验证

## 相关资源

- [Google AdMob 快速入门](https://developers.google.com/admob/android/quick-start)
- [插页式广告指南](https://developers.google.com/admob/android/interstitial)
- [用户消息传递平台 (UMP) SDK](https://developers.google.com/admob/ump/android/quick-start)
- [测试广告](https://developers.google.com/admob/android/test-ads)

## 代码文件清单

### 新增文件
- `app/src/main/java/com/example/compose/jetchat/ads/GoogleMobileAdsConsentManager.kt`
- `app/src/main/java/com/example/compose/jetchat/ads/InterstitialAdManager.kt`

### 修改文件
- `app/build.gradle.kts` - 添加依赖
- `app/src/main/AndroidManifest.xml` - 添加权限和 App ID
- `app/src/main/java/com/example/compose/jetchat/NavActivity.kt` - 集成广告逻辑

## 注意事项

1. **隐私合规：** 确保遵守 GDPR、CCPA 等隐私法规
2. **用户体验：** 不要过度展示广告，影响用户体验
3. **测试：** 在生产环境发布前，充分测试广告功能
4. **监控：** 使用 AdMob 控制台监控广告表现和收益
5. **更新：** 定期更新 AdMob SDK 到最新版本

