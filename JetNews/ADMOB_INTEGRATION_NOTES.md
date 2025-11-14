# AdMob App Open Ad 集成说明

## 已完成的工作

1. ✅ Gradle 配置：添加了 play-services-ads、UMP SDK、MultiDex 和 lifecycle-process 依赖
2. ✅ AndroidManifest：配置了 MyApplication、AdMob App ID、SplashActivity 作为启动入口
3. ✅ AppOpenAdManager：实现了广告加载、显示、缓存和生命周期管理
4. ✅ MyApplication：继承 MultiDexApplication，集成 AppOpenAdManager，注册生命周期监听
5. ✅ GoogleMobileAdsConsentManager：实现了 UMP 同意收集逻辑
6. ✅ SplashActivity：实现了倒计时、UMP 同意、广告加载显示流程
7. ✅ MainActivity：添加了隐私设置入口和 Ad Inspector 调用

## 调试/生产注意事项

### 1. 测试广告位 ID

当前使用的是 Google 官方测试广告位 ID：
- **App Open Ad Unit ID**: `ca-app-pub-3940256099942544/3419835294`
- **AdMob App ID**: `ca-app-pub-3940256099942544~3347511713`

**位置**：
- `AppOpenAdManager.kt` 中的 `AD_UNIT_ID` 常量
- `AndroidManifest.xml` 中的 `com.google.android.gms.ads.APPLICATION_ID` meta-data

### 2. 更换正式广告位 ID

**生产环境必须替换**：

1. **在 AdMob 控制台创建 App Open Ad Unit**：
   - 登录 [AdMob 控制台](https://apps.admob.com/)
   - 创建新的 App Open Ad Unit
   - 复制 Ad Unit ID（格式：`ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX`）

2. **替换 AppOpenAdManager.kt 中的 AD_UNIT_ID**：
```kotlin
// 在 AppOpenAdManager.kt 中
private const val AD_UNIT_ID = "ca-app-pub-你的发布者ID/你的广告位ID"
```

3. **替换 AndroidManifest.xml 中的 APPLICATION_ID**：
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-你的发布者ID~你的应用ID"/>
```

### 3. RequestConfiguration 配置

当前在 `SplashActivity.initializeMobileAdsSdk()` 中配置了测试设备 ID。

**生产环境建议**：
- 移除或注释掉测试设备 ID 配置
- 或者根据构建变体动态配置

```kotlin
if (BuildConfig.DEBUG) {
    MobileAds.setRequestConfiguration(
        RequestConfiguration.Builder()
            .setTestDeviceIds(listOf(MyApplication.TEST_DEVICE_HASHED_ID))
            .build()
    )
}
```

### 4. 测试设备 ID

应用启动时会在 Logcat 中输出当前设备的测试 ID，格式：
```
Test Device ID: XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
Add this to your test device list in AdMob console
```

**使用方法**：
1. 运行应用，查看 Logcat 输出
2. 复制测试设备 ID
3. 在 AdMob 控制台的 "Test devices" 中添加该 ID
4. 或者直接在代码中使用 `MyApplication.TEST_DEVICE_HASHED_ID`

### 5. 前后台切换重载策略

**当前实现**：
- 通过 `ProcessLifecycleOwner` 监听应用生命周期
- 通过 `ActivityLifecycleCallbacks` 监听 Activity 生命周期
- 应用从后台回到前台时，如果广告可用且未过期（4小时内），会自动显示

**广告缓存有效期**：
- App Open Ad 的有效期为 4 小时
- 超过 4 小时的广告会被视为过期，需要重新加载

**重载逻辑**：
- 广告显示后自动加载新广告
- 广告加载失败后会自动重试
- 应用启动时如果已有缓存的广告，会先显示，然后加载新广告

### 6. 崩溃防护

**已实现的保护措施**：

1. **空值检查**：
   - `AppOpenAdManager.onStart()` 中检查 `currentActivity` 是否为 null
   - 广告显示前检查广告对象是否存在

2. **状态检查**：
   - 防止重复加载广告（`isLoadingAd` 标志）
   - 防止重复显示广告（`isShowingAd` 标志）

3. **异常处理**：
   - `FullScreenContentCallback` 中处理广告显示失败的情况
   - UMP 同意收集失败时的错误日志记录

4. **生命周期管理**：
   - `SplashActivity` 中正确取消 `CountDownTimer`
   - Activity 销毁时清理资源

### 7. 日志要点

**关键日志标签**：
- `MyApplication`: Application 初始化日志
- `AppOpenAdManager`: 广告加载和显示日志
- `SplashActivity`: Splash 页面和初始化日志
- `GoogleMobileAdsConsentManager`: UMP 同意收集日志
- `MainActivity`: 隐私设置和 Ad Inspector 日志

**调试时关注**：
- `onAdLoaded`: 广告加载成功
- `onAdFailedToLoad`: 广告加载失败（检查网络、Ad Unit ID、UMP 同意状态）
- `onAdShowedFullScreenContent`: 广告显示成功
- `onAdFailedToShowFullScreenContent`: 广告显示失败
- `onAdDismissedFullScreenContent`: 广告关闭

### 8. UMP (User Messaging Platform) 配置

**测试 GDPR 地区**：
在 `GoogleMobileAdsConsentManager.gatherConsent()` 中取消注释以下行：
```kotlin
.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
```

**生产环境**：
- 移除或注释掉 `setDebugGeography` 调用
- UMP 会根据用户的实际地理位置自动判断是否需要显示同意表单

### 9. 隐私设置入口

**MainActivity 菜单**：
- "Privacy Settings": 显示 UMP 隐私选项表单（仅在需要时）
- "Ad Inspector": 打开 AdMob Ad Inspector（用于调试）

**使用场景**：
- 用户想要修改隐私设置
- 开发者需要调试广告问题

### 10. 常见问题排查

**广告不显示**：
1. 检查网络连接
2. 确认 Ad Unit ID 正确
3. 检查 UMP 同意状态（`canRequestAds`）
4. 查看 Logcat 中的错误日志
5. 确认测试设备 ID 已添加到 AdMob 控制台（测试环境）

**广告加载失败**：
1. 检查 `onAdFailedToLoad` 日志中的错误代码和消息
2. 常见错误：
   - ERROR_CODE_NETWORK_ERROR: 网络问题
   - ERROR_CODE_NO_FILL: 没有可用的广告
   - ERROR_CODE_INVALID_REQUEST: Ad Unit ID 错误

**UMP 表单不显示**：
1. 检查是否在 GDPR 地区
2. 确认 UMP 配置正确
3. 检查 `gatherConsent` 回调中的错误信息

### 11. 性能优化建议

1. **预加载广告**：在 SplashActivity 中尽早开始加载广告
2. **缓存管理**：利用 4 小时缓存有效期，避免频繁加载
3. **后台加载**：使用协程在后台线程初始化 MobileAds SDK
4. **避免阻塞**：广告加载和显示不阻塞主线程

### 12. 发布前检查清单

- [ ] 替换测试 Ad Unit ID 为正式 ID
- [ ] 替换测试 AdMob App ID 为正式 ID
- [ ] 移除或条件化测试设备 ID 配置
- [ ] 移除 UMP DebugGeography 配置（生产环境）
- [ ] 测试广告在不同网络条件下的表现
- [ ] 测试 GDPR 地区的 UMP 流程
- [ ] 验证隐私设置入口正常工作
- [ ] 检查 ProGuard 规则（如使用代码混淆）
- [ ] 测试应用前后台切换时的广告显示
- [ ] 验证崩溃防护机制

## ProGuard 规则（如使用代码混淆）

如果启用了代码混淆，需要在 `proguard-rules.pro` 中添加：

```
# Google Mobile Ads
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# UMP SDK
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.ump.**
```

## 参考资源

- [AdMob App Open Ads 官方文档](https://developers.google.com/admob/android/app-open)
- [UMP SDK 文档](https://developers.google.com/admob/android/privacy)
- [Google Mobile Ads SDK 示例](https://github.com/googleads/googleads-mobile-android-examples)

