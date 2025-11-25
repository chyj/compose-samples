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

package com.example.jetlagged

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.jetlagged.ui.theme.JetLaggedTheme
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash Activity
 * 负责启动流程：MobileAds 初始化 -> 加载广告 -> 显示广告 -> 跳转到主界面
 */
class SplashActivity : ComponentActivity() {
    companion object {
        private const val TAG = "AdMob_Splash"
        private const val AD_LOAD_TIMEOUT_MS = 5000L // 广告加载超时 5 秒
        private const val SPLASH_TIMEOUT_MS = 10000L // 启动超时 10 秒（总超时保护）
    }

    private var startTime: Long = 0
    private var isAdShown = false
    private var isInitializationComplete = false
    private var isAdLoaded = false
    private var isAdLoadFailed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        startTime = System.currentTimeMillis()
        Log.d(TAG, "onCreate: SplashActivity 创建")

        setContent {
            JetLaggedTheme {
                SplashScreen(
                    onTimeout = {
                        // 超时后直接跳转
                        navigateToMain()
                    }
                )
            }
        }

        // 开始启动流程
        lifecycleScope.launch {
            initializeMobileAds()
        }
    }

    private fun initializeMobileAds() {
        if (isInitializationComplete) {
            Log.d(TAG, "initializeMobileAds: 已经初始化，跳过")
            loadAndShowAd()
            return
        }

        Log.d(TAG, "initializeMobileAds: 步骤 1 - 初始化 MobileAds")
        MobileAds.initialize(this) { initializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            Log.d(TAG, "initializeMobileAds: MobileAds 初始化完成")
            statusMap.forEach { (adapter, status) ->
                Log.d(TAG, "initializeMobileAds: 适配器 $adapter - 状态: ${status.initializationState}, " +
                        "延迟: ${status.latency}ms, 描述: ${status.description}")
            }
            isInitializationComplete = true
            loadAndShowAd()
        }
    }

    private fun loadAndShowAd() {
        Log.d(TAG, "loadAndShowAd: 步骤 2 - 加载并尝试显示广告")
        val app = application as? MyApplication
        
        // 加载广告，并监听加载结果
        app?.appOpenAdManager?.loadAd(
            onAdLoaded = {
                Log.d(TAG, "loadAndShowAd: 广告加载成功，立即尝试显示")
                isAdLoaded = true
                // 广告加载成功后立即显示，不等待固定时间
                showAdIfAvailable()
            },
            onAdFailedToLoad = {
                Log.d(TAG, "loadAndShowAd: 广告加载失败，直接跳转")
                isAdLoadFailed = true
                // 广告加载失败时直接跳转，不等待固定时间
                navigateToMain()
            }
        )

        // 设置超时保护：如果广告加载时间过长，超时后跳转
        lifecycleScope.launch {
            delay(AD_LOAD_TIMEOUT_MS) // 等待5秒
            if (!isAdShown && !isAdLoadFailed) {
                Log.w(TAG, "loadAndShowAd: 广告加载超时（${AD_LOAD_TIMEOUT_MS}ms），尝试显示已加载的广告")
                // 超时后尝试显示可能已加载的广告
                showAdIfAvailable()
                // 如果还是没有显示，直接跳转
                if (!isAdShown) {
                    Log.w(TAG, "loadAndShowAd: 广告仍未显示，直接跳转")
                    isAdLoadFailed = true
                    navigateToMain()
                }
            }
        }
    }

    private fun showAdIfAvailable() {
        Log.d(TAG, "showAdIfAvailable: 步骤 3 - 尝试显示广告")
        val app = application as? MyApplication
        
        // 设置广告关闭回调
        val adShown = app?.appOpenAdManager?.showAdIfAvailable(this) { 
            // 广告关闭后的回调
            Log.d(TAG, "showAdIfAvailable: 广告已关闭，立即跳转")
            // 广告关闭后立即跳转，不等待固定时间
            navigateToMain()
        } == true

        if (adShown) {
            Log.d(TAG, "showAdIfAvailable: 广告已显示，等待用户关闭")
            isAdShown = true
            // 广告显示后，等待广告关闭回调，然后跳转
        } else {
            Log.d(TAG, "showAdIfAvailable: 广告不可用，直接跳转")
            // 广告不可用时直接跳转，不等待固定时间
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        if (isFinishing) {
            Log.d(TAG, "navigateToMain: Activity 已结束，跳过跳转")
            return
        }

        Log.d(TAG, "navigateToMain: 跳转到 MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}

@Composable
private fun SplashScreen(onTimeout: () -> Unit) {
    var countdown by remember { mutableStateOf(10) }

    LaunchedEffect(Unit) {
        // 10 秒超时保护
        delay(10000L)
        onTimeout()
    }

    LaunchedEffect(Unit) {
        // 倒计时显示（10秒）
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "JetLagged\n\n启动中...\n\n$countdown",
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

