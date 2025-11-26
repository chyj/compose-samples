# AdMob Banner 集成说明

本文档说明如何在 Reply 应用中集成 AdMob Banner 广告。

## 已完成的集成步骤

### 1. 依赖配置

已在 `app/build.gradle.kts` 中添加以下依赖：
```kotlin
implementation("com.google.android.gms:play-services-ads:24.4.0")
implementation("com.google.android.ump:user-messaging-platform:3.2.0")
```

### 2. AndroidManifest 配置

已在 `AndroidManifest.xml` 中声明 AdMob App ID：
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-3940256099942544~3347511713"/>
```

**注意**：请将测试 App ID 替换为您在 AdMob 后台创建的实际 App ID。

### 3. SDK 初始化

在 `MainActivity.onCreate()` 中已初始化：
- MobileAds SDK
- GoogleMobileAdsConsentManager（合规授权管理器）

### 4. Banner 广告组件

已创建 `BannerAd` Composable，位于 `app/src/main/java/com/example/reply/ui/ads/BannerAd.kt`

### 5. 界面集成

Banner 广告已集成到 `ReplyEmailList` 中，显示在邮件列表底部。

## 使用方法

### 替换测试广告位 ID

在 `ReplyListContent.kt` 中，将测试广告位 ID 替换为您的实际广告位 ID：

```kotlin
BannerAd(
    adUnitId = "ca-app-pub-3940256099942544/6300978111", // 替换为您的实际广告位 ID
    modifier = Modifier.padding(vertical = 8.dp)
)
```

### 注册测试设备

如需注册测试设备，在 `MainActivity.initializeMobileAds()` 中取消注释并配置：

```kotlin
val testDeviceIds = listOf("YOUR_TEST_DEVICE_ID")
val requestConfiguration = RequestConfiguration.Builder()
    .setTestDeviceIds(testDeviceIds)
    .build()
MobileAds.setRequestConfiguration(requestConfiguration)
```

**获取测试设备 ID**：
1. 运行应用
2. 在 logcat 中搜索 "Use RequestConfiguration.Builder().setTestDeviceIds"
3. 复制显示的设备 ID

### 合规授权配置

如需配置 GDPR/CCPA 合规授权，在 `GoogleMobileAdsConsentManager.kt` 中：

1. 取消注释调试设置以测试授权流程：
```kotlin
val debugSettings = ConsentDebugSettings.Builder(activity)
    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
    .addTestDeviceHashedId("YOUR_TEST_DEVICE_ID")
    .build()
```

2. 在 AdMob 后台配置授权表单

## Logcat 筛选关键字

在 Android Studio 的 Logcat 中使用以下关键字筛选广告相关日志：

### 主要关键字：
- `AdMob` - 所有 AdMob 相关日志
- `BannerAd` - Banner 广告生命周期日志
- `AdMobConsent` - 合规授权相关日志
- `MainActivity` - SDK 初始化日志

### 具体筛选命令：
```
tag:AdMob OR tag:BannerAd OR tag:AdMobConsent OR tag:MainActivity
```

### 关键日志事件：
- `初始化 MobileAds SDK` - SDK 开始初始化
- `MobileAds SDK 初始化完成` - SDK 初始化完成
- `创建 BannerAd` - Banner 组件创建
- `开始加载广告请求` - 开始请求广告
- `Banner 广告加载成功` - 广告加载成功
- `Banner 广告加载失败` - 广告加载失败（包含错误详情）
- `Banner 广告被点击` - 用户点击广告
- `Banner 广告展示` - 广告展示
- `清理 AdView 资源` - 组件销毁时清理资源

## 常见问题排查

### 1. 广告不显示
- 检查 logcat 中的错误日志（使用 `tag:BannerAd` 筛选）
- 确认 `canRequestAds` 为 `true`（检查合规授权状态）
- 确认网络连接正常
- 确认广告位 ID 正确

### 2. 合规授权问题
- 检查 `AdMobConsent` 标签的日志
- 确认在 AdMob 后台已配置授权表单
- 测试时可以使用调试地理设置

### 3. 测试设备注册
- 查看 logcat 中的设备 ID 提示
- 确保设备 ID 格式正确（32 位十六进制字符串）

## 文件清单

已创建/修改的文件：
- `app/src/main/java/com/example/reply/ui/ads/GoogleMobileAdsConsentManager.kt` - 合规授权管理器
- `app/src/main/java/com/example/reply/ui/ads/BannerAd.kt` - Banner 广告组件
- `app/src/main/java/com/example/reply/ui/MainActivity.kt` - 添加 SDK 初始化
- `app/src/main/java/com/example/reply/ui/ReplyListContent.kt` - 集成 Banner 广告
- `app/build.gradle.kts` - 添加依赖（已存在）
- `app/src/main/AndroidManifest.xml` - 添加 App ID（已存在）

## 下一步操作

1. **替换 App ID**：在 `AndroidManifest.xml` 中替换为您的实际 App ID
2. **创建广告位**：在 AdMob 后台创建 Banner 广告位
3. **替换广告位 ID**：在 `ReplyListContent.kt` 中替换为实际广告位 ID
4. **配置合规授权**：如需 GDPR/CCPA 支持，在 AdMob 后台配置授权表单
5. **测试**：使用测试广告位 ID 验证功能正常后，再切换到正式广告位

