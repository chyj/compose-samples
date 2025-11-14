# 任务歧义问题修复说明

## 问题描述

当项目有多个 product flavors 时，运行未指定 flavor 的任务（如 `assembleDebugUnitTest`）会出现歧义错误：

```
Cannot locate tasks that match ':app:assembleDebugUnitTest' as task 'assembleDebugUnitTest' is ambiguous in project ':app'. 
Candidates are: 'assembleBannerDebugUnitTest', 'assembleDefaultDebugUnitTest'.
```

## 解决方案

已在 `app/build.gradle.kts` 中配置了 `missingDimensionStrategy`，指定当 flavor 未明确指定时，默认使用 `default` flavor。

### 配置位置

1. **defaultConfig**（全局默认）：
```kotlin
defaultConfig {
    // ...
    missingDimensionStrategy("version", "default")
}
```

2. **buildTypes**（特定构建类型）：
```kotlin
buildTypes {
    getByName("debug") {
        missingDimensionStrategy("version", "default")
    }
    getByName("release") {
        missingDimensionStrategy("version", "default")
    }
}
```

## 如何应用修复

### 1. 同步 Gradle 项目

在 Android Studio 中：
- 点击 **File → Sync Project with Gradle Files**
- 或点击工具栏的 **Sync** 按钮（🔄）

### 2. 清理并重新构建

```bash
./gradlew clean
./gradlew build
```

## 验证修复

### 方法 1: 运行未指定 flavor 的任务

```bash
# 应该不再报错，会自动使用 default flavor
./gradlew assembleDebugUnitTest

# 其他任务也应该正常工作
./gradlew test
./gradlew assembleDebug
```

### 方法 2: 查看可用任务

```bash
# 查看所有测试相关任务
./gradlew tasks --all | grep -i "test"

# 应该能看到：
# assembleDefaultDebugUnitTest
# assembleBannerDebugUnitTest
# assembleDefaultReleaseUnitTest
# assembleBannerReleaseUnitTest
```

## 工作原理

当运行 `assembleDebugUnitTest` 时：

1. Gradle 检测到 `version` dimension 未指定
2. 查找 `missingDimensionStrategy` 配置
3. 找到 `defaultConfig` 和 `buildTypes` 中的配置
4. 使用 `"default"` 作为默认 flavor
5. 任务解析为 `assembleDefaultDebugUnitTest`

## 明确指定 Flavor

如果需要构建特定 flavor 的版本，可以明确指定：

```bash
# Default flavor
./gradlew assembleDefaultDebugUnitTest
./gradlew assembleDefaultDebug

# Banner flavor
./gradlew assembleBannerDebugUnitTest
./gradlew assembleBannerDebug
```

## 如果问题仍然存在

### 1. 确保已同步项目
- Android Studio: File → Sync Project with Gradle Files
- 或运行: `./gradlew --refresh-dependencies`

### 2. 清理构建缓存
```bash
./gradlew clean
rm -rf .gradle
rm -rf app/build
```

### 3. 检查配置
确保 `app/build.gradle.kts` 中包含：
- `flavorDimensions += "version"`
- `productFlavors` 中定义了 `default` 和 `banner`
- `defaultConfig` 中有 `missingDimensionStrategy("version", "default")`
- `buildTypes` 的 `debug` 和 `release` 中也有 `missingDimensionStrategy("version", "default")`

### 4. 重启 Android Studio
有时需要重启 IDE 才能完全应用配置更改。

## 相关文件

- `app/build.gradle.kts` - 主要配置文件
- `PRODUCT_FLAVORS_SETUP.md` - Product Flavors 详细说明

