# Product Flavors 配置说明

## 概述

项目现在支持产品变体（Product Flavors），类似于 `nowinandroid` 项目的配置方式。

## 变体配置

### 1. default 变体
- **Application ID**: `com.example.jetnews`
- **Version Name**: `1.0`
- **用途**: 标准版本

### 2. banner 变体
- **Application ID**: `com.example.jetnews.banner`
- **Version Name**: `1.0-banner`
- **用途**: Banner 广告版本

## 构建变体

### 可用的构建变体组合

- `defaultDebug` - 标准版本 Debug
- `defaultRelease` - 标准版本 Release
- `bannerDebug` - Banner 版本 Debug
- `bannerRelease` - Banner 版本 Release

## 如何构建

### 使用 Gradle 命令行

```bash
# 构建 banner Debug 版本
./gradlew assembleBannerDebug

# 构建 banner Release 版本
./gradlew assembleBannerRelease

# 安装 banner Debug 版本
./gradlew installBannerDebug

# 构建所有变体
./gradlew assembleAll
```

### 使用 Android Studio

1. 打开 **Build Variants** 窗口：
   - View → Tool Windows → Build Variants
   - 或点击左下角的 **Build Variants** 标签

2. 选择变体：
   - 在 **Active Build Variant** 列中，选择 `bannerDebug` 或 `bannerRelease`

3. 构建：
   - Build → Make Project
   - 或 Build → Build Bundle(s) / APK(s)

## google-services.json 配置

### 文件位置

如果项目使用 Firebase，需要为每个 flavor 配置 `google-services.json`：

```
app/
├── google-services.json          # 默认配置（包含所有变体）
└── src/
    ├── main/
    ├── banner/
    │   └── google-services.json  # Banner 变体特定配置（可选）
    └── default/
        └── google-services.json  # Default 变体特定配置（可选）
```

### 配置方式

#### 方式 1: 单一 google-services.json（推荐）

在 `app/google-services.json` 中包含所有变体的配置：

```json
{
  "project_info": {
    "project_number": "123456789012",
    "project_id": "your-project-id"
  },
  "client": [
    {
      "client_info": {
        "android_client_info": {
          "package_name": "com.example.jetnews"
        }
      }
    },
    {
      "client_info": {
        "android_client_info": {
          "package_name": "com.example.jetnews.banner"
        }
      }
    }
  ]
}
```

#### 方式 2: 变体特定的 google-services.json

为每个变体创建独立的配置文件：

- `app/src/default/google-services.json` - default 变体
- `app/src/banner/google-services.json` - banner 变体

### 获取 google-services.json

1. 登录 [Firebase Console](https://console.firebase.google.com/)
2. 选择项目
3. 进入 Project Settings → General
4. 在 "Your apps" 部分，为每个 package name 添加 Android 应用：
   - `com.example.jetnews`
   - `com.example.jetnews.banner`
5. 下载对应的 `google-services.json` 文件
6. 合并配置或分别放置到对应目录

## AdMob 配置

### AndroidManifest.xml 配置

每个变体可以有不同的 AdMob App ID：

#### Default 变体
在 `app/src/main/AndroidManifest.xml` 中配置默认的 AdMob App ID。

#### Banner 变体
在 `app/src/banner/AndroidManifest.xml` 中覆盖 AdMob App ID：

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application>
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-你的发布者ID~你的banner应用ID"/>
    </application>
</manifest>
```

### 在 AdMob 控制台配置

1. 登录 [AdMob Console](https://apps.admob.com/)
2. 为每个 Application ID 创建对应的应用：
   - `com.example.jetnews` → 创建应用 A
   - `com.example.jetnews.banner` → 创建应用 B
3. 为每个应用创建对应的 Ad Unit ID
4. 在代码中根据变体使用不同的 Ad Unit ID

## 变体特定的资源

### 目录结构

```
app/src/
├── main/              # 共享资源
│   ├── AndroidManifest.xml
│   ├── java/
│   └── res/
├── banner/            # Banner 变体特定资源
│   ├── AndroidManifest.xml
│   └── res/
│       └── values/
│           └── strings.xml
└── default/           # Default 变体特定资源（可选）
    └── res/
```

### 示例：为 banner 变体添加不同的应用名称

创建 `app/src/banner/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">JetNews Banner</string>
</resources>
```

### 示例：为 banner 变体添加不同的图标

将图标文件放置到：
- `app/src/banner/res/mipmap-hdpi/ic_launcher.png`
- `app/src/banner/res/mipmap-mdpi/ic_launcher.png`
- `app/src/banner/res/mipmap-xhdpi/ic_launcher.png`
- 等等...

## 代码中访问变体信息

### 在代码中判断当前变体

```kotlin
import com.example.jetnews.BuildConfig

// 检查是否是 banner 变体
if (BuildConfig.FLAVOR == "banner") {
    // Banner 变体特定逻辑
}

// 获取完整的 Application ID
val applicationId = BuildConfig.APPLICATION_ID
// default: "com.example.jetnews"
// banner: "com.example.jetnews.banner"
```

### 根据变体使用不同的 Ad Unit ID

在 `AppOpenAdManager.kt` 中：

```kotlin
companion object {
    private const val AD_UNIT_ID_DEFAULT = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
    private const val AD_UNIT_ID_BANNER = "ca-app-pub-YYYYYYYYYYYYYYYY/YYYYYYYYYY"
    
    private val AD_UNIT_ID: String
        get() = when (BuildConfig.FLAVOR) {
            "banner" -> AD_UNIT_ID_BANNER
            else -> AD_UNIT_ID_DEFAULT
        }
}
```

## 验证构建变体

### 检查 APK 包名

```bash
# 检查 default 变体
aapt dump badging app/build/outputs/apk/default/debug/app-default-debug.apk | grep package
# 应该显示: package: name='com.example.jetnews'

# 检查 banner 变体
aapt dump badging app/build/outputs/apk/banner/debug/app-banner-debug.apk | grep package
# 应该显示: package: name='com.example.jetnews.banner'
```

### 检查版本名称

```bash
aapt dump badging app/build/outputs/apk/banner/debug/app-banner-debug.apk | grep versionName
# 应该显示: versionName='1.0-banner'
```

## 注意事项

1. **namespace**: 所有变体共享相同的 `namespace` (`com.example.jetnews`)，只有 `applicationId` 不同

2. **代码共享**: 所有变体共享相同的源代码，只有资源文件可以变体特定

3. **同时安装**: 两个变体可以独立安装在同一设备上（因为 Application ID 不同）

4. **Firebase**: 如果使用 Firebase，确保 `google-services.json` 包含所有变体的配置

5. **AdMob**: 确保在 AdMob 控制台为每个 Application ID 创建对应的应用

## 常见问题

### Q: 如何为不同变体使用不同的依赖？
A: 在 `build.gradle.kts` 中使用 `bannerImplementation` 或 `defaultImplementation`

### Q: 如何在不同变体中使用不同的代码？
A: 使用 `BuildConfig.FLAVOR` 在代码中判断，或创建变体特定的源文件目录

### Q: google-services.json 必须放在哪里？
A: 
- 单一文件：`app/google-services.json`
- 变体特定：`app/src/{flavor}/google-services.json`

### Q: 如何验证变体配置是否正确？
A: 构建后检查 APK 的包名和版本名称

## 相关文件

- `app/build.gradle.kts` - 变体配置
- `app/src/main/AndroidManifest.xml` - 默认清单文件
- `app/src/banner/AndroidManifest.xml` - Banner 变体清单文件
- `app/google-services.json` - Firebase 配置文件（如果使用）
- `app/google-services.json.example` - 配置文件示例

