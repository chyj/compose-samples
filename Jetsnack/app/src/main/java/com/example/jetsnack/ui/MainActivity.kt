/*
 * Copyright 2020-2025 The Android Open Source Project
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

package com.example.jetsnack.ui

import android.appwidget.AppWidgetManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.example.jetsnack.ads.GoogleMobileAdsConsentManager
import com.example.jetsnack.widget.RecentOrdersWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // 初始化 AdMob SDK 和用户同意管理
        initializeAdMob()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            lifecycleScope.launch(Dispatchers.Default) {
                setWidgetPreviews()
            }
        }
        setContent { JetsnackApp() }
    }

    /**
     * 初始化 AdMob SDK 和用户同意管理
     */
    private fun initializeAdMob() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 收集用户同意（可选，根据您的需求决定是否启用）
                // GoogleMobileAdsConsentManager.collectConsent(
                //     this@MainActivity,
                //     onConsentFormLoadSuccess = {
                //         Log.d("MainActivity", "用户同意收集成功")
                //     },
                //     onConsentFormLoadFailure = { error ->
                //         Log.e("MainActivity", "用户同意收集失败: $error")
                //     }
                // )
                Log.d("MainActivity", "AdMob SDK 初始化完成")
            } catch (e: Exception) {
                Log.e("MainActivity", "AdMob 初始化失败", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    suspend fun setWidgetPreviews() {
        val receiver = RecentOrdersWidgetReceiver::class
        val installedProviders = getSystemService(AppWidgetManager::class.java).installedProviders
        val providerInfo = installedProviders.firstOrNull {
            it.provider.className ==
                receiver.qualifiedName
        }
        providerInfo?.generatedPreviewCategories.takeIf { it == 0 }?.let {
            // Set previews if this provider if unset
            GlanceAppWidgetManager(this).setWidgetPreviews(receiver)
        }
    }
}
