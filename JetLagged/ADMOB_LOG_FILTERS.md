# AdMob App Open Ad 日志筛选关键字

在 Android Studio 的 Logcat 中，可以使用以下关键字来筛选和查看 AdMob App Open Ad 相关的日志：

## 主要日志标签（TAG）

### 1. 应用级别日志
- **关键字**: `AdMob_App`
- **说明**: MyApplication 的生命周期和 Activity 回调日志
- **示例**: Application 创建、Activity 生命周期事件

### 2. App Open Ad 管理器日志
- **关键字**: `AdMob_AppOpen`
- **说明**: AppOpenAdManager 的广告加载、显示、状态管理日志
- **示例**: 
  - 广告加载成功/失败
  - 广告显示/关闭
  - 广告过期检查
  - ResponseInfo 信息

### 3. UMP 同意表单日志
- **关键字**: `AdMob_UMP`
- **说明**: GoogleMobileAdsConsentManager 的同意表单相关日志
- **示例**:
  - 同意信息更新
  - 同意表单加载/显示
  - 用户同意状态

### 4. Splash Activity 日志
- **关键字**: `AdMob_Splash`
- **说明**: SplashActivity 的启动流程日志
- **示例**:
  - 初始化流程各步骤
  - MobileAds 初始化
  - 广告加载和显示流程

### 5. Settings 屏幕日志
- **关键字**: `AdMob_Settings`
- **说明**: SettingsScreen 的用户操作日志
- **示例**:
  - 打开隐私设置
  - 打开 Ad Inspector
  - 重置同意状态

## Logcat 筛选器配置

### 方式 1: 使用单个标签筛选
在 Logcat 的搜索框中输入：
```
tag:AdMob_AppOpen
```

### 方式 2: 使用多个标签筛选（推荐）
在 Logcat 的搜索框中输入：
```
tag:AdMob_AppOpen | tag:AdMob_UMP | tag:AdMob_Splash | tag:AdMob_App | tag:AdMob_Settings
```

### 方式 3: 使用包名筛选
```
package:com.example.jetlagged
```

### 方式 4: 使用正则表达式（高级）
```
tag:AdMob_.*
```

## 关键日志事件

### 广告加载流程
1. `AdMob_AppOpen: loadAd: 开始加载广告`
2. `AdMob_AppOpen: loadAd: 广告加载成功` 或 `AdMob_AppOpen: onAdFailedToLoad`
3. `AdMob_AppOpen: ResponseInfo` - 查看广告响应信息

### 广告显示流程
1. `AdMob_AppOpen: showAdIfAvailable: 尝试显示广告`
2. `AdMob_AppOpen: onAdShowedFullScreenContent: 广告已显示`
3. `AdMob_AppOpen: onAdDismissedFullScreenContent: 广告已关闭`

### 同意表单流程
1. `AdMob_UMP: requestConsentInfoUpdate: 开始请求同意信息`
2. `AdMob_UMP: loadAndShowConsentForm: 开始加载同意表单`
3. `AdMob_UMP: loadAndShowConsentForm: 表单已关闭，用户同意状态`

### 应用生命周期
1. `AdMob_App: onCreate: Application 创建`
2. `AdMob_App: onActivityStarted` - Activity 启动
3. `AdMob_App: onStart: 应用进程进入前台` - 可能触发广告显示

## 调试技巧

1. **查看完整流程**: 使用 `tag:AdMob_.*` 查看所有 AdMob 相关日志
2. **排查加载问题**: 重点关注 `AdMob_AppOpen` 和 `AdMob_UMP` 标签
3. **检查生命周期**: 使用 `AdMob_App` 标签查看应用和 Activity 生命周期
4. **查看错误**: 搜索 `ERROR` 或 `onAdFailedToLoad` 查看失败原因

## 常见问题排查

### 广告不显示
- 检查 `AdMob_UMP: canRequestAds` 是否为 true
- 查看 `AdMob_AppOpen: isAdAvailable` 日志
- 确认 `AdMob_AppOpen: onAdFailedToLoad` 是否有错误

### 同意表单不显示
- 检查 `AdMob_UMP: requestConsentInfoUpdate` 是否成功
- 查看 `AdMob_UMP: loadAndShowConsentForm` 的错误信息

### 广告过期
- 查看 `AdMob_AppOpen: isAdAvailable: 广告已过期` 日志
- 检查 `loadTime` 和当前时间的差值

