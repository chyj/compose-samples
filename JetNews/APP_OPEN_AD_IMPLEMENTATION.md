# Google AdMob App Open Ad 集成实施方案

本文档基于官方 Quick Start 和 App Open 指南，参考 Google 官方示例项目，为 JetNews 项目提供详细的 App Open Ad 集成方案。

---

## 1. Gradle 依赖与版本要求

### 1.1 添加依赖到 `app/build.gradle.kts`

在 `dependencies` 块中添加以下依赖：

```kotlin
// Google Mobile Ads SDK
implementation("com.google.android.gms:play-services-ads:23.5.0")

// User Messaging Platform (UMP) - 用于 GDPR/CCPA 同意收集
implementation("com.google.android.ump:user-messaging-platform:2.2.0")

// MultiDex 支持（如果方法数超过 64K）
implementation("androidx.multidex:multidex:2.0.1")

// Lifecycle 扩展（用于 ProcessLifecycleOwner）
implementation("androidx.lifecycle:lifecycle-process:2.8.2")
```

### 1.2 版本要求检查

在 `app/build.gradle.kts` 的 `android` 块中确认：

```kotlin
android {
    compileSdk = 36  // 当前项目已满足
    
    defaultConfig {
        minSdk = 23  // AdMob 最低要求 21，建议 23+
        targetSdk = 33  // 当前项目已满足
        
        // 如果启用 MultiDex
        multiDexEnabled = true
    }
    
    kotlinOptions {
        jvmTarget = "17"  // 当前项目已满足
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

### 1.3 更新 `gradle/libs.versions.toml`

在 `[versions]` 块中添加：

```toml
play-services-ads = "23.5.0"
user-messaging-platform = "2.2.0"
multidex = "2.0.1"
lifecycle-process = "2.8.2"
```

在 `[libraries]` 块中添加：

```toml
play-services-ads = { module = "com.google.android.gms:play-services-ads", version.ref = "play-services-ads" }
user-messaging-platform = { module = "com.google.android.ump:user-messaging-platform", version.ref = "user-messaging-platform" }
multidex = { module = "androidx.multidex:multidex", version.ref = "multidex" }
lifecycle-process = { module = "androidx.lifecycle:lifecycle-process", version.ref = "lifecycle-process" }
```

---

## 2. Manifest 改动

### 2.1 添加权限

在 `AndroidManifest.xml` 的 `<manifest>` 标签内添加（如果尚未存在）：

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
```

### 2.2 AdMob App ID meta-data

在 `<application>` 标签内添加：

```xml
<!-- 测试 App ID，生产环境需替换为正式 ID -->
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-3940256099942544~3347511713"/>
```

**重要**：生产环境必须使用 AdMob 后台获取的真实 App ID，格式为 `ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX`。

### 2.3 SplashActivity 配置

将 `MainActivity` 的 `LAUNCHER` intent-filter 移除，改为 `SplashActivity`：

```xml
<!-- SplashActivity 作为启动页 -->
<activity
    android:name=".SplashActivity"
    android:exported="true"
    android:theme="@style/Theme.Jetnews.Splash">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<!-- MainActivity 移除 LAUNCHER，保留其他 intent-filter -->
<activity
    android:name=".ui.MainActivity"
    android:exported="false"
    android:windowSoftInputMode="adjustResize">
    <!-- 移除 MAIN/LAUNCHER intent-filter -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.BROWSABLE"/>
        <data
            android:host="developer.android.com"
            android:pathPrefix="/jetnews"
            android:scheme="https" />
    </intent-filter>
</activity>
```

### 2.4 创建 Splash Theme

在 `res/values/themes.xml` 中添加：

```xml
<style name="Theme.Jetnews.Splash" parent="Theme.Jetnews">
    <item name="android:windowBackground">@android:color/white</item>
    <item name="android:windowNoTitle">true</item>
    <item name="android:windowActionBar">false</item>
    <item name="android:windowFullscreen">false</item>
    <item name="android:windowContentOverlay">@null</item>
</style>
```

---

## 3. Application 层：AppOpenAdManager 封装

### 3.1 创建 AppOpenAdManager 类

创建 `app/src/main/java/com/example/jetnews/AppOpenAdManager.kt`：

```kotlin
package com.example.jetnews

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

/**
 * App Open Ad 管理器
 * 
 * 功能：
 * - 加载和缓存 App Open Ad
 * - 管理广告有效期（4小时）
 * - 在应用从后台恢复时展示广告
 * - 错误处理和日志记录
 */
class AppOpenAdManager(private val application: Application) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "AppOpenAdManager"
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/3419835294" // 测试广告位
        private const val AD_EXPIRY_HOURS = 4L
        private const val AD_EXPIRY_MILLIS = AD_EXPIRY_HOURS * 60 * 60 * 1000
        
        // 测试设备 ID（生产环境移除或使用真实设备 ID）
        const val TEST_DEVICE_HASHED_ID = "YOUR_TEST_DEVICE_ID"
    }

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var loadTime: Long = 0
    private var currentActivity: Activity? = null

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /**
     * 设置当前 Activity（由 Application 的 ActivityLifecycleCallbacks 调用）
     */
    fun setCurrentActivity(activity: Activity?) {
        currentActivity = activity
    }

    /**
     * 加载广告
     */
    fun loadAd(context: Context) {
        // 如果广告已可用或正在加载，则跳过
        if (isAdAvailable() || isLoadingAd) {
            return
        }

        isLoadingAd = true
        val request = AdRequest.Builder().build()

        AppOpenAd.load(
            context,
            AD_UNIT_ID,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "App Open Ad loaded successfully")
                    appOpenAd = ad
                    loadTime = Date().time
                    isLoadingAd = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "App Open Ad failed to load: ${loadAdError.message}")
                    isLoadingAd = false
                    // 失败后不自动重试，避免频繁请求
                }
            }
        )
    }

    /**
     * 如果广告可用则展示
     * 
     * @param activity 展示广告的 Activity
     * @param onShowAdCompleteListener 广告展示完成回调
     */
    fun showAdIfAvailable(
        activity: Activity,
        onShowAdCompleteListener: OnShowAdCompleteListener
    ) {
        // 如果当前 Activity 是 SplashActivity，不展示广告
        if (activity is SplashActivity) {
            onShowAdCompleteListener.onShowAdComplete()
            return
        }

        // 如果广告不可用，尝试加载并直接完成
        if (!isAdAvailable()) {
            Log.d(TAG, "App Open Ad not available, loading new ad")
            loadAd(activity)
            onShowAdCompleteListener.onShowAdComplete()
            return
        }

        // 设置回调并展示广告
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "App Open Ad dismissed")
                appOpenAd = null
                onShowAdCompleteListener.onShowAdComplete()
                // 广告关闭后加载新广告
                loadAd(activity)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "App Open Ad failed to show: ${adError.message}")
                appOpenAd = null
                onShowAdCompleteListener.onShowAdComplete()
                // 失败后加载新广告
                loadAd(activity)
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "App Open Ad showed")
            }

            override fun onAdImpression() {
                Log.d(TAG, "App Open Ad impression recorded")
            }
        }

        appOpenAd?.show(activity)
    }

    /**
     * 检查广告是否可用
     * - 广告对象存在
     - 未过期（4小时内）
     */
    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(AD_EXPIRY_HOURS)
    }

    /**
     * 检查加载时间是否在指定小时数内
     */
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    /**
     * 应用从后台恢复时调用（ProcessLifecycleOwner.onStart）
     */
    override fun onStart(owner: LifecycleOwner) {
        currentActivity?.let { activity ->
            if (activity !is SplashActivity) {
                showAdIfAvailable(activity) {
                    // 广告展示完成后的回调（如果需要）
                }
            }
        }
    }

    /**
     * 广告展示完成监听器
     */
    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }
}
```

### 3.2 更新 JetnewsApplication

修改 `JetnewsApplication.kt`：

```kotlin
package com.example.jetnews

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.multidex.MultiDexApplication
import com.example.jetnews.data.AppContainer
import com.example.jetnews.data.AppContainerImpl

class JetnewsApplication : MultiDexApplication(), Application.ActivityLifecycleCallbacks {
    companion object {
        const val JETNEWS_APP_URI = "https://developer.android.com/jetnews"
    }

    // AppContainer instance used by the rest of classes to obtain dependencies
    lateinit var container: AppContainer

    // App Open Ad Manager
    lateinit var appOpenAdManager: AppOpenAdManager

    override fun onCreate() {
        super.onCreate()
        container = AppContainerImpl(this)
        appOpenAdManager = AppOpenAdManager(this)
        
        // 注册 Activity 生命周期回调
        registerActivityLifecycleCallbacks(this)
    }

    // ActivityLifecycleCallbacks 实现
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // 更新 AppOpenAdManager 的当前 Activity
        appOpenAdManager.setCurrentActivity(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        appOpenAdManager.setCurrentActivity(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        appOpenAdManager.setCurrentActivity(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        // 不清理 currentActivity，因为可能在后台
    }

    override fun onActivityStopped(activity: Activity) {
        // 不清理 currentActivity，因为可能在后台
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // 不需要处理
    }

    override fun onActivityDestroyed(activity: Activity) {
        // Activity 销毁时不需要特殊处理
        // currentActivity 会在下一个 Activity 创建/启动时自动更新
    }
}
```

---

## 4. GoogleMobileAdsConsentManager：UMP 同意收集

### 4.1 更新 GoogleMobileAdsConsentManager

修改 `app/src/main/java/com/example/jetnews/GoogleMobileAdsConsentManager.kt`：

```kotlin
package com.example.jetnews

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm.OnConsentFormDismissedListener
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

/**
 * Google Mobile Ads 同意管理器
 * 
 * 使用 UMP SDK 处理 GDPR/CCPA 同意收集流程
 */
class GoogleMobileAdsConsentManager private constructor(context: Context) {
    
    companion object {
        private const val TAG = "ConsentManager"
        
        @Volatile
        private var instance: GoogleMobileAdsConsentManager? = null

        fun getInstance(context: Context): GoogleMobileAdsConsentManager {
            return instance ?: synchronized(this) {
                instance ?: GoogleMobileAdsConsentManager(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    /**
     * 是否可以请求广告
     */
    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    /**
     * 是否需要显示隐私选项表单
     */
    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * 收集用户同意
     * 
     * @param activity 当前 Activity
     * @param onConsentGatheringCompleteListener 完成回调
     */
    fun gatherConsent(
        activity: Activity,
        onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener
    ) {
        // 调试设置（仅用于测试）
        val debugSettings = ConsentDebugSettings.Builder(activity)
            // 取消注释以下行以强制测试地理位置
            // .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            // 添加测试设备 ID
            .addTestDeviceHashedId(AppOpenAdManager.TEST_DEVICE_HASHED_ID)
            .build()

        val params = ConsentRequestParameters.Builder()
            .setConsentDebugSettings(debugSettings)
            .build()

        // 每次应用启动时请求更新同意信息
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // 如果需要，加载并显示同意表单
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "${formError.errorCode}: ${formError.message}")
                    }
                    // 同意收集完成
                    onConsentGatheringCompleteListener.consentGatheringComplete(formError)
                }
            },
            { requestConsentError ->
                Log.w(TAG, "${requestConsentError.errorCode}: ${requestConsentError.message}")
                onConsentGatheringCompleteListener.consentGatheringComplete(requestConsentError)
            }
        )
    }

    /**
     * 显示隐私选项表单
     * 
     * 用于在设置页面中允许用户修改同意选择
     */
    fun showPrivacyOptionsForm(
        activity: Activity,
        onConsentFormDismissedListener: OnConsentFormDismissedListener
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(
            activity,
            onConsentFormDismissedListener
        )
    }

    /**
     * 同意收集完成监听器
     */
    fun interface OnConsentGatheringCompleteListener {
        fun consentGatheringComplete(error: FormError?)
    }
}
```

---

## 5. SplashActivity：启动流程与广告展示

### 5.1 创建 SplashActivity

创建/更新 `app/src/main/java/com/example/jetnews/SplashActivity.kt`：

```kotlin
package com.example.jetnews

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.jetnews.ui.MainActivity
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 启动页 Activity
 * 
 * 流程：
 * 1. 显示启动页（倒计时）
 * 2. 收集用户同意（UMP）
 * 3. 如果同意，初始化 MobileAds SDK 并加载广告
 * 4. 倒计时结束后展示广告（如果可用）并跳转主界面
 */
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SplashActivity"
        // 倒计时时间（毫秒）
        private const val COUNTER_TIME_MILLISECONDS = 5000L
    }

    private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private val gatherConsentFinished = AtomicBoolean(false)
    private var secondsRemaining: Long = 0L
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 如果使用 Compose，可以设置 Compose 内容
        // 这里假设使用传统布局，如果需要 Compose 可以修改
        setContentView(R.layout.activity_splash)

        // 记录 Mobile Ads SDK 版本
        Log.d(TAG, "Google Mobile Ads SDK Version: ${MobileAds.getVersion()}")

        // 初始化同意管理器
        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(this)

        // 创建倒计时器
        createTimer()

        // 收集用户同意
        googleMobileAdsConsentManager.gatherConsent(this) { consentError ->
            if (consentError != null) {
                Log.w(TAG, "${consentError.errorCode}: ${consentError.message}")
            }

            gatherConsentFinished.set(true)

            // 如果可以请求广告，初始化 MobileAds SDK
            if (googleMobileAdsConsentManager.canRequestAds) {
                initializeMobileAdsSdk()
            }

            // 如果倒计时已结束，启动主界面
            if (secondsRemaining <= 0) {
                startMainActivity()
            }
        }

        // 如果之前已有同意，立即初始化（不等待表单）
        if (googleMobileAdsConsentManager.canRequestAds) {
            initializeMobileAdsSdk()
        }
    }

    /**
     * 创建倒计时器
     */
    private fun createTimer() {
        countDownTimer = object : CountDownTimer(COUNTER_TIME_MILLISECONDS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1
                // 更新 UI（如果有 TextView）
                // counterTextView.text = "App is done loading in: $secondsRemaining"
                Log.d(TAG, "Seconds remaining: $secondsRemaining")
            }

            override fun onFinish() {
                secondsRemaining = 0
                Log.d(TAG, "Timer finished")

                // 展示广告（如果可用）
                val app = application as JetnewsApplication
                app.appOpenAdManager.showAdIfAvailable(
                    this@SplashActivity,
                    object : AppOpenAdManager.OnShowAdCompleteListener {
                        override fun onShowAdComplete() {
                            // 确保同意收集已完成后再跳转
                            if (gatherConsentFinished.get()) {
                                startMainActivity()
                            }
                        }
                    }
                )
            }
        }
        countDownTimer?.start()
    }

    /**
     * 初始化 MobileAds SDK
     */
    private fun initializeMobileAdsSdk() {
        // 防止重复初始化
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }

        // 配置测试设备
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AppOpenAdManager.TEST_DEVICE_HASHED_ID))
                .build()
        )

        // 在后台线程初始化 SDK
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                MobileAds.initialize(this@SplashActivity) { initializationStatus ->
                    Log.d(TAG, "MobileAds initialized: ${initializationStatus.adapterStatusMap}")
                }
            }

            // 在主线程加载广告
            withContext(Dispatchers.Main) {
                val app = application as JetnewsApplication
                app.appOpenAdManager.loadAd(this@SplashActivity)
            }
        }
    }

    /**
     * 启动主界面
     */
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
```

### 5.2 创建 SplashActivity 布局（可选）

**方案 A：使用传统 XML 布局**

创建 `app/src/main/res/layout/activity_splash.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@android:color/white">

    <TextView
        android:id="@+id/timer"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Loading..."
        android:textSize="18sp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

然后在 `SplashActivity.onCreate()` 中使用：
```kotlin
setContentView(R.layout.activity_splash)
```

**方案 B：使用 Compose（推荐，与项目风格一致）**

修改 `SplashActivity.onCreate()`，使用 `MutableState` 管理倒计时状态：

```kotlin
class SplashActivity : AppCompatActivity() {
    // ... 其他属性
    
    // 使用 MutableState 以便 Compose 自动更新
    private var secondsRemainingState = mutableStateOf(0L)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "JetNews",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (secondsRemainingState.value > 0) 
                            "Loading in: ${secondsRemainingState.value}" 
                        else "Done.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        
        // ... 其余初始化代码
    }
    
    private fun createTimer() {
        countDownTimer = object : CountDownTimer(COUNTER_TIME_MILLISECONDS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1
                secondsRemainingState.value = secondsRemaining // 更新 Compose 状态
            }
            
            override fun onFinish() {
                secondsRemaining = 0
                secondsRemainingState.value = 0
                // ... 其余逻辑
            }
        }
        countDownTimer?.start()
    }
}
```

需要添加的导入：
```kotlin
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
```

**注意**：`AppCompatActivity` 和 `ComponentActivity` 都支持 Compose。如果项目使用 Material3 和 AppCompat，建议使用 `AppCompatActivity`；如果使用纯 Material3，可以使用 `ComponentActivity`。

---

## 6. MainActivity 中的隐私设置/Ad Inspector 入口

### 6.1 在 MainActivity 中添加隐私设置入口

修改 `app/src/main/java/com/example/jetnews/ui/MainActivity.kt`：

```kotlin
package com.example.jetnews.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.example.jetnews.JetnewsApplication
import com.example.jetnews.GoogleMobileAdsConsentManager
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appContainer = (application as JetnewsApplication).container
        val consentManager = GoogleMobileAdsConsentManager.getInstance(this)
        
        setContent {
            val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass
            JetnewsApp(
                appContainer = appContainer,
                widthSizeClass = widthSizeClass,
                onShowPrivacyOptions = {
                    // 显示隐私选项表单
                    if (consentManager.isPrivacyOptionsRequired) {
                        consentManager.showPrivacyOptionsForm(this) {
                            // 表单关闭后的回调
                        }
                    }
                },
                onOpenAdInspector = {
                    // 打开 Ad Inspector（用于调试）
                    MobileAds.openAdInspector(this) { adInspectorError ->
                        if (adInspectorError != null) {
                            // 处理错误
                        }
                    }
                }
            )
        }
    }
}
```

### 6.2 在 Compose UI 中添加设置入口

需要在 `JetnewsApp` 或设置页面中添加按钮调用上述回调。示例：

```kotlin
// 在设置页面或菜单中
Button(onClick = { onShowPrivacyOptions() }) {
    Text("隐私设置")
}

Button(onClick = { onOpenAdInspector() }) {
    Text("Ad Inspector")
}
```

---

## 7. 调试与生产注意事项

### 7.1 测试广告位 ID

**测试期间使用**：
- App Open Ad: `ca-app-pub-3940256099942544/3419835294`
- App ID: `ca-app-pub-3940256099942544~3347511713`

**生产环境替换**：
1. 登录 [AdMob 控制台](https://apps.admob.com/)
2. 创建应用并获取 App ID
3. 创建 App Open Ad 广告单元并获取 Ad Unit ID
4. 替换 `AppOpenAdManager.AD_UNIT_ID` 和 `AndroidManifest.xml` 中的 App ID

### 7.2 RequestConfiguration 配置

在 `SplashActivity.initializeMobileAdsSdk()` 中配置：

```kotlin
MobileAds.setRequestConfiguration(
    RequestConfiguration.Builder()
        .setTestDeviceIds(listOf("YOUR_TEST_DEVICE_ID")) // 生产环境移除
        .setTagForChildDirectedTreatment(
            RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(
            RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED
        )
        .build()
)
```

**获取测试设备 ID**：
```kotlin
// 在 Logcat 中搜索 "Use RequestConfiguration.Builder().setTestDeviceIds"
// 会输出类似 "33BE2250B02818" 的设备 ID
```

### 7.3 前后台切换重载策略

- **从后台恢复**：`ProcessLifecycleOwner.onStart()` 触发，`AppOpenAdManager` 自动展示广告
- **广告过期**：4 小时后自动失效，需要重新加载
- **广告展示后**：自动加载新广告，为下次展示做准备

### 7.4 崩溃防护

在 `AppOpenAdManager` 中添加 try-catch：

```kotlin
fun showAdIfAvailable(...) {
    try {
        // 展示广告逻辑
    } catch (e: Exception) {
        Log.e(TAG, "Error showing ad", e)
        onShowAdCompleteListener.onShowAdComplete()
    }
}
```

### 7.5 日志观察点

关键日志标签：
- `AppOpenAdManager`: 广告加载和展示
- `SplashActivity`: 启动流程
- `ConsentManager`: 同意收集
- `MobileAds`: SDK 初始化

使用 Logcat 过滤：
```bash
adb logcat -s AppOpenAdManager SplashActivity ConsentManager MobileAds
```

**获取测试设备 ID 的详细步骤**：

1. **方法一：通过 Logcat 自动输出**
   - 运行应用（不设置测试设备 ID）
   - 在 Logcat 中搜索 "Use RequestConfiguration.Builder().setTestDeviceIds"
   - 会看到类似输出：`To get test ads on this device, set your test device ID to "33BE2250B02818"`
   - 复制引号中的设备 ID

2. **方法二：通过代码获取**
   在 `SplashActivity` 或 `AppOpenAdManager` 中添加：
   ```kotlin
   // 获取所有测试设备 ID
   val testDeviceIds = MobileAds.getRequestConfiguration().testDeviceIds
   Log.d(TAG, "Test Device IDs: $testDeviceIds")
   ```

3. **方法三：使用 Ad Inspector**
   - 在 MainActivity 中调用 `MobileAds.openAdInspector()`
   - 在 Ad Inspector 界面中可以看到设备信息

**重要**：生产环境必须移除或注释掉测试设备 ID 配置。

### 7.6 生产环境检查清单

- [ ] 替换测试 App ID 为真实 App ID
- [ ] 替换测试 Ad Unit ID 为真实 Ad Unit ID
- [ ] 移除或注释测试设备 ID
- [ ] 移除 UMP 调试地理位置设置
- [ ] 测试 GDPR/CCPA 同意流程
- [ ] 测试前后台切换广告展示
- [ ] 验证广告展示不影响用户体验
- [ ] 检查 ProGuard 规则（如启用混淆）

### 7.7 ProGuard 规则

在 `app/proguard-rules.pro` 中添加：

```proguard
# Google Mobile Ads
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# UMP SDK
-keep class com.google.android.ump.** { *; }
```

---

## 8. 合并到已有自定义 Application 或启动页

### 8.1 如果已有自定义 Application

如果 `JetnewsApplication` 已有其他逻辑，按以下方式合并：

1. **继承 MultiDexApplication**（如果尚未继承）
2. **实现 ActivityLifecycleCallbacks**（如果尚未实现）
3. **添加 AppOpenAdManager 实例**
4. **在 onCreate 中初始化 AppOpenAdManager**
5. **在生命周期回调中更新 currentActivity**

参考第 3.2 节的 `JetnewsApplication` 更新代码。

### 8.2 如果已有启动页

如果已有启动页逻辑：

1. **重命名现有启动页为 SplashActivity**（如果不同名）
2. **添加 UMP 同意收集流程**
3. **添加 MobileAds 初始化**
4. **在适当时机调用 `appOpenAdManager.showAdIfAvailable()`**
5. **确保广告展示后再跳转主界面**

关键点：
- 同意收集和广告加载可以并行进行
- 倒计时结束后再展示广告
- 确保同意收集完成后再跳转

---

## 9. 常见问题与排查建议

### 9.1 广告不显示

**可能原因**：
1. App ID 或 Ad Unit ID 错误
2. 未初始化 MobileAds SDK
3. 未获得用户同意（GDPR/CCPA）
4. 网络问题
5. 广告未加载完成

**排查步骤**：
1. 检查 Logcat 中的错误日志
2. 确认 `MobileAds.initialize()` 已调用
3. 确认 `canRequestAds` 为 `true`
4. 检查网络连接
5. 使用测试广告位验证

### 9.2 同意表单不显示

**可能原因**：
1. UMP 后台未配置
2. 测试设备未设置调试地理位置
3. 之前已同意/拒绝

**排查步骤**：
1. 在 UMP 后台配置同意表单
2. 取消注释 `setDebugGeography(DEBUG_GEOGRAPHY_EEA)`
3. 清除应用数据重新测试

### 9.3 应用崩溃

**可能原因**：
1. MultiDex 未启用（方法数超限）
2. ProGuard 规则缺失
3. 空指针异常

**排查步骤**：
1. 检查 `multiDexEnabled = true`
2. 添加 ProGuard 规则
3. 检查 `currentActivity` 是否为 null

### 9.4 广告频繁展示

**可能原因**：
1. 前后台切换频繁
2. 广告有效期检查失效

**解决方案**：
- 确保 `wasLoadTimeLessThanNHoursAgo()` 正确实现
- 考虑添加最小展示间隔（如 1 小时）

### 9.5 测试设备 ID 获取

**方法**：
1. 运行应用
2. 在 Logcat 中搜索 "Use RequestConfiguration.Builder().setTestDeviceIds"
3. 复制输出的设备 ID

### 9.6 性能优化建议

1. **延迟加载**：不在应用启动时立即加载，等待用户同意后
2. **后台预加载**：在后台时预加载广告，提升展示速度
3. **缓存管理**：及时清理过期广告，避免内存泄漏
4. **错误重试**：失败后使用指数退避策略重试

---

## 10. 完整代码文件清单

需要创建/修改的文件：

1. ✅ `app/build.gradle.kts` - 添加依赖
2. ✅ `gradle/libs.versions.toml` - 添加版本定义
3. ✅ `app/src/main/AndroidManifest.xml` - 添加权限、meta-data、SplashActivity
4. ✅ `app/src/main/res/values/themes.xml` - 添加 Splash Theme
5. ✅ `app/src/main/java/com/example/jetnews/AppOpenAdManager.kt` - 新建
6. ✅ `app/src/main/java/com/example/jetnews/JetnewsApplication.kt` - 修改
7. ✅ `app/src/main/java/com/example/jetnews/GoogleMobileAdsConsentManager.kt` - 修改
8. ✅ `app/src/main/java/com/example/jetnews/SplashActivity.kt` - 创建/修改
9. ✅ `app/src/main/java/com/example/jetnews/ui/MainActivity.kt` - 修改（添加隐私设置入口）
10. ✅ `app/proguard-rules.pro` - 添加 ProGuard 规则（如启用混淆）

---

## 11. 参考资源

- [AdMob Quick Start](https://developers.google.com/admob/android/quick-start?hl=zh-CN)
- [App Open Ad 指南](https://developers.google.com/admob/android/app-open?hl=zh-cn)
- [官方示例代码](https://github.com/googleads/googleads-mobile-android-examples/tree/main/kotlin/admob/AppOpenExample)
- [UMP SDK 文档](https://developers.google.com/admob/ump/android/quick-start)
- [Ad Inspector 文档](https://developers.google.com/admob/android/ad-inspector)

---

**实施完成后，请务必进行充分测试，确保广告正常展示且不影响用户体验。**

