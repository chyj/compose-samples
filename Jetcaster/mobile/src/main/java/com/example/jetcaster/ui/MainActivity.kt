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
import com.example.jetcaster.ads.GoogleMobileAdsConsentManager
import com.example.jetcaster.glancewidget.updateWidgetPreview
import com.example.jetcaster.ui.theme.JetcasterTheme
import com.google.accompanist.adaptive.calculateDisplayFeatures
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentForm
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化 AdMob SDK 和用户同意管理
        initializeAdMob()

        enableEdgeToEdge()
        updateWidgetPreview(this)
        setContent {
            val displayFeatures = calculateDisplayFeatures(this)

            JetcasterTheme {
                JetcasterApp(
                    displayFeatures,
                )
            }
        }
    }

    /**
     * 初始化 AdMob SDK 和用户同意管理
     */
    private fun initializeAdMob() {
        // 初始化用户同意管理器
        val consentManager = GoogleMobileAdsConsentManager.getInstance()
        
        // 检查是否为调试模式
        // 注意：在生产环境中，应该根据实际需求设置 isDebug
        val isDebug = false // 设置为 true 以启用调试模式（测试地理位置等）
        consentManager.initialize(this, isDebug)

        // 初始化 AdMob SDK
        activityScope.launch(Dispatchers.IO) {
            MobileAds.initialize(this@MainActivity) { initializationStatus ->
                Log.d(TAG, "AdMob SDK initialized: ${initializationStatus.adapterStatusMap}")
                
                // SDK 初始化完成后，检查是否需要显示同意表单
                activityScope.launch(Dispatchers.Main) {
                    if (consentManager.isConsentFormAvailable() && 
                        !consentManager.canRequestAds()) {
                        // 需要显示同意表单
                        consentManager.loadConsentForm(
                            this@MainActivity,
                            object : ConsentForm.OnConsentFormDismissedListener {
                                override fun onConsentFormDismissed(formError: com.google.android.ump.FormError?) {
                                    if (formError != null) {
                                        Log.e(TAG, "Consent form error: ${formError.message}")
                                    } else {
                                        Log.d(TAG, "Consent form dismissed successfully")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
