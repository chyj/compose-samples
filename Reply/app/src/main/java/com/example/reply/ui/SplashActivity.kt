/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.reply.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.reply.ui.theme.ContrastAwareReplyTheme
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

private const val TAG = "OpenAdLifecycle"
private const val MIN_WAIT_TIME_MS = 3000L
private const val MAX_WAIT_TIME_MS = 5000L

class SplashActivity : ComponentActivity() {

    private var startTime: Long = 0
    private var isAdDismissed = false
    private val handler = Handler(Looper.getMainLooper())
    private var minWaitRunnable: Runnable? = null
    private var maxWaitRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "SplashActivity: onCreate")
        startTime = System.currentTimeMillis()

        // 标记 SplashActivity 活跃，防止 AppOpenAdManager 自动显示广告
        (application as ReplyApplication).appOpenAdManager.setSplashScreenActive(true)

        setContent {
            ContrastAwareReplyTheme {
                SplashScreen()
            }
        }

        initializeMobileAdsSdk()
    }

    private fun initializeMobileAdsSdk() {
        Log.d(TAG, "initializeMobileAdsSdk: Starting initialization")

        // 设置测试设备 ID
        val testDeviceIds = listOf("TEST_DEVICE_ID")
        val configuration = RequestConfiguration.Builder()
            .setTestDeviceIds(testDeviceIds)
            .build()
        MobileAds.setRequestConfiguration(configuration)

        // 创建同意管理器
        val consentManager = GoogleMobileAdsConsentManager(this)

        // 收集同意（测试环境跳过对话框）
        consentManager.gatherConsent(skipConsentDialog = true) {
            Log.d(TAG, "initializeMobileAdsSdk: Consent gathered")
            // 初始化 MobileAds SDK（无论同意状态如何，测试环境都应该初始化）
            Log.d(TAG, "initializeMobileAdsSdk: Initializing MobileAds SDK")
            MobileAds.initialize(this) { initializationStatus ->
                Log.d(TAG, "initializeMobileAdsSdk: MobileAds SDK initialized")
                val appOpenAdManager = (application as ReplyApplication).appOpenAdManager
                appOpenAdManager.setCurrentActivity(this)

                // 加载广告
                appOpenAdManager.loadAd { success ->
                    Log.d(TAG, "initializeMobileAdsSdk: Ad loaded: $success")
                    if (success) {
                        // 广告加载成功，立即显示
                        checkAndShowAd()
                    } else {
                        // 广告加载失败，等待最小时间后跳转
                        onAdDismissed()
                    }
                }
            }
        }

        // 设置超时保护
        maxWaitRunnable = Runnable {
            Log.d(TAG, "initializeMobileAdsSdk: Max wait time reached")
            if (!isAdDismissed) {
                goToMainActivity()
            }
        }
        handler.postDelayed(maxWaitRunnable!!, MAX_WAIT_TIME_MS)
    }

    private fun checkAndShowAd() {
        Log.d(TAG, "checkAndShowAd: Checking ad availability")
        val appOpenAdManager = (application as ReplyApplication).appOpenAdManager

        if (appOpenAdManager.isAdAvailable()) {
            Log.d(TAG, "checkAndShowAd: Ad available, showing")
            appOpenAdManager.showAdIfAvailable(this) {
                Log.d(TAG, "checkAndShowAd: Ad dismissed")
                onAdDismissed()
            }
        } else {
            Log.d(TAG, "checkAndShowAd: Ad not available")
            onAdDismissed()
        }
    }

    private fun onAdDismissed() {
        if (isAdDismissed) {
            return
        }
        isAdDismissed = true

        val elapsedTime = System.currentTimeMillis() - startTime
        val remainingTime = MIN_WAIT_TIME_MS - elapsedTime

        if (remainingTime > 0) {
            Log.d(TAG, "onAdDismissed: Waiting remaining time: $remainingTime ms")
            minWaitRunnable = Runnable {
                goToMainActivity()
            }
            handler.postDelayed(minWaitRunnable!!, remainingTime)
        } else {
            Log.d(TAG, "onAdDismissed: Minimum wait time already passed")
            goToMainActivity()
        }
    }

    private fun goToMainActivity() {
        if (isAdDismissed) {
            Log.d(TAG, "goToMainActivity: Navigating to MainActivity")
            minWaitRunnable?.let { handler.removeCallbacks(it) }
            maxWaitRunnable?.let { handler.removeCallbacks(it) }
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "SplashActivity: onDestroy")
        // 取消 SplashActivity 活跃标记
        (application as ReplyApplication).appOpenAdManager.setSplashScreenActive(false)
        minWaitRunnable?.let { handler.removeCallbacks(it) }
        maxWaitRunnable?.let { handler.removeCallbacks(it) }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

