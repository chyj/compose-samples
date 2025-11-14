# AdMob 错误代码 3 故障排除指南

## 错误信息

```
ERROR_CODE_3: Ad unit doesn't match format
Message: Ad unit doesn't match format. <https://support.google.com/admob/answer/9905175#4>
```

## 错误原因分析

错误代码 3 通常表示以下问题之一：

1. **Ad Unit ID 不是 App Open Ad 类型**
   - 使用了其他广告类型（Banner、Interstitial、Rewarded）的 Ad Unit ID
   - App Open Ad 需要专门的 Ad Unit ID

2. **Ad Unit ID 格式不正确**
   - 格式应为：`ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX`
   - 当前使用的测试 ID：`ca-app-pub-3940256099942544/3419835294`

3. **Ad Unit ID 在 AdMob 账户中不存在**
   - 测试 ID 可能在某些地区不可用
   - 需要先在 AdMob 控制台创建 App Open Ad Unit

## 解决方案

### 方案 1: 在 AdMob 控制台创建 App Open Ad Unit（推荐）

1. **登录 AdMob 控制台**
   - 访问：https://apps.admob.com/
   - 使用您的 Google 账户登录

2. **创建 App Open Ad Unit**
   - 选择您的应用（或创建新应用）
   - 进入 "Ad units" 页面
   - 点击 "Add ad unit"
   - 选择 "App Open" 广告类型
   - 填写广告单元名称
   - 点击 "Create ad unit"

3. **复制 Ad Unit ID**
   - 创建后会显示 Ad Unit ID
   - 格式：`ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX`
   - 复制这个 ID

4. **更新代码中的 AD_UNIT_ID**
   ```kotlin
   // 在 AppOpenAdManager.kt 中
   private const val AD_UNIT_ID = "ca-app-pub-你的发布者ID/你的广告位ID"
   ```

### 方案 2: 验证测试 Ad Unit ID

如果使用测试 Ad Unit ID，请确认：

1. **测试 ID 格式**
   ```
   ca-app-pub-3940256099942544/3419835294
   ```

2. **检查 AdMob App ID**
   - 在 `AndroidManifest.xml` 中确认 App ID 正确
   - 当前测试 App ID：`ca-app-pub-3940256099942544~3347511713`

3. **验证网络连接**
   - 确保设备可以访问 AdMob 服务器
   - 检查防火墙设置

### 方案 3: 使用正式 Ad Unit ID（生产环境）

**重要**: 生产环境必须使用您在 AdMob 控制台创建的正式 Ad Unit ID。

1. **创建正式 App Open Ad Unit**
   - 在 AdMob 控制台创建
   - 确保广告类型为 "App Open"

2. **更新代码**
   ```kotlin
   // AppOpenAdManager.kt
   private const val AD_UNIT_ID = "ca-app-pub-你的发布者ID/你的AppOpen广告位ID"
   ```

3. **更新 AndroidManifest.xml**
   ```xml
   <meta-data
       android:name="com.google.android.gms.ads.APPLICATION_ID"
       android:value="ca-app-pub-你的发布者ID~你的应用ID"/>
   ```

## 调试步骤

### 1. 检查日志输出

运行应用后，查看 Logcat 中的详细错误信息：

```bash
adb logcat AppOpenAdManager:E *:S
```

您应该看到：
```
AppOpenAdManager: ERROR_CODE_3: Ad unit doesn't match format
AppOpenAdManager: ERROR_CODE_3: Current AD_UNIT_ID: ca-app-pub-3940256099942544/3419835294
AppOpenAdManager: ERROR_CODE_3: Please verify in AdMob console that this is an App Open Ad Unit ID
```

### 2. 验证 Ad Unit ID 格式

代码中已添加格式验证，会在日志中显示验证结果：

```
AppOpenAdManager: loadAd() - Ad Unit ID format validation: PASSED
```

如果格式验证失败，会显示：
```
AppOpenAdManager: loadAd() - Invalid Ad Unit ID format: ...
AppOpenAdManager: loadAd() - Expected format: ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
```

### 3. 检查 AdMob 控制台

1. 登录 AdMob 控制台
2. 选择您的应用
3. 进入 "Ad units" 页面
4. 确认是否存在 App Open Ad Unit
5. 如果不存在，创建一个新的 App Open Ad Unit

### 4. 验证 App ID

检查 `AndroidManifest.xml` 中的 App ID：

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-3940256099942544~3347511713"/>
```

确保：
- App ID 格式正确：`ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX`
- App ID 在 AdMob 控制台中存在
- App ID 与应用包名匹配

## 常见错误代码对照表

| 错误代码 | 含义 | 解决方案 |
|---------|------|---------|
| 0 | ERROR_CODE_INTERNAL_ERROR | 内部错误，重试 |
| 1 | ERROR_CODE_INVALID_REQUEST | Ad Unit ID 不正确 |
| 2 | ERROR_CODE_NETWORK_ERROR | 网络连接问题 |
| 3 | Ad unit doesn't match format | **当前错误** - Ad Unit ID 格式或类型不匹配 |
| 8 | ERROR_CODE_NO_FILL | 没有可用的广告 |

## 测试建议

### 开发阶段

1. **使用测试设备**
   - 在代码中设置测试设备 ID
   - 在 AdMob 控制台添加测试设备

2. **使用测试 Ad Unit ID**
   - 测试 ID：`ca-app-pub-3940256099942544/3419835294`
   - 如果测试 ID 不工作，创建自己的测试 Ad Unit

3. **检查日志**
   - 使用 Logcat 过滤查看详细错误信息
   - 关注 `AppOpenAdManager` 标签的日志

### 生产环境

1. **创建正式 Ad Unit**
   - 在 AdMob 控制台创建 App Open Ad Unit
   - 使用正式的 Ad Unit ID

2. **移除测试配置**
   - 移除或条件化测试设备 ID
   - 使用正式 App ID

3. **监控错误**
   - 在 AdMob 控制台查看广告表现
   - 监控错误率和填充率

## 相关资源

- [AdMob App Open Ads 文档](https://developers.google.com/admob/android/app-open)
- [AdMob 错误代码参考](https://developers.google.com/admob/android/reference/com/google/android/gms/ads/AdRequest#ERROR_CODE_INVALID_REQUEST)
- [AdMob 支持页面](https://support.google.com/admob/answer/9905175#4)

## 下一步

1. ✅ 检查 AdMob 控制台，确认 App Open Ad Unit 是否存在
2. ✅ 如果不存在，创建一个新的 App Open Ad Unit
3. ✅ 更新代码中的 `AD_UNIT_ID` 为实际的 Ad Unit ID
4. ✅ 重新运行应用，查看日志输出
5. ✅ 如果仍有问题，检查网络连接和 AdMob App ID

