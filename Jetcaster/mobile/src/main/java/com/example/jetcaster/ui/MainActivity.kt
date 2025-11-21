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

package com.example.jetcaster.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import com.example.jetcaster.ads.GoogleMobileAdsConsentManager
import com.example.jetcaster.glancewidget.updateWidgetPreview
import com.example.jetcaster.ui.theme.JetcasterTheme
import com.google.accompanist.adaptive.calculateDisplayFeatures
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var consentManager: GoogleMobileAdsConsentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化合规管理器
        consentManager = GoogleMobileAdsConsentManager.getInstance(this)

        enableEdgeToEdge()
        updateWidgetPreview(this)
        setContent {
            val displayFeatures = calculateDisplayFeatures(this)

            // 请求合规信息更新
            LaunchedEffect(Unit) {
                try {
                    // 仅在首次启动时请求合规信息
                    consentManager.requestConsentInfoUpdate(
                        activity = this@MainActivity,
                        isDebug = false, // 生产环境设为 false
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "初始化合规流程失败", e)
                }
            }

            JetcasterTheme {
                JetcasterApp(
                    displayFeatures,
                    consentManager = consentManager,
                )
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
