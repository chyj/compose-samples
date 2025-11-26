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

package com.example.reply.ui.ads

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Google Mobile Ads 合规授权管理器
 * 用于处理 GDPR/CCPA 等合规要求
 */
class GoogleMobileAdsConsentManager private constructor() {
    
    private var consentInformation: ConsentInformation? = null
    private var consentForm: ConsentForm? = null
    
    companion object {
        private const val TAG = "AdMobConsent"
        
        @Volatile
        private var INSTANCE: GoogleMobileAdsConsentManager? = null
        
        fun getInstance(): GoogleMobileAdsConsentManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GoogleMobileAdsConsentManager().also { INSTANCE = it }
            }
        }
    }
    
    /**
     * 是否可以请求广告
     */
    val canRequestAds: Boolean
        get() = consentInformation?.canRequestAds() == true
    
    /**
     * 初始化合规信息
     */
    fun initialize(activity: Activity) {
        Log.d(TAG, "初始化合规授权管理器")
        
        // 创建调试设置（仅在测试时使用）
        val debugSettings = ConsentDebugSettings.Builder(activity)
            // 添加测试设备 ID（从 logcat 中获取）
            // .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            // .addTestDeviceHashedId("YOUR_TEST_DEVICE_ID")
            .build()
        
        val params = ConsentRequestParameters.Builder()
            .setConsentDebugSettings(debugSettings)
            .build()
        
        consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        
        consentInformation?.requestConsentInfoUpdate(
            activity,
            params,
            {
                Log.d(TAG, "合规信息更新成功，canRequestAds: ${consentInformation?.canRequestAds()}")
                // 如果需要显示授权表单
                if (consentInformation?.isConsentFormAvailable() == true) {
                    loadConsentForm(activity)
                }
            },
            { formError ->
                Log.e(TAG, "合规信息更新失败: ${formError.message}")
            }
        )
    }
    
    /**
     * 加载授权表单
     */
    private fun loadConsentForm(activity: Activity) {
        Log.d(TAG, "加载授权表单")
        UserMessagingPlatform.loadConsentForm(
            activity,
            { form ->
                consentForm = form
                Log.d(TAG, "授权表单加载成功")
                // 如果需要立即显示表单，可以在这里调用 showConsentForm
            },
            { formError ->
                Log.e(TAG, "授权表单加载失败: ${formError.message}")
            }
        )
    }
    
    /**
     * 显示授权表单
     */
    fun showConsentForm(activity: Activity, onConsentFormDismissed: () -> Unit) {
        Log.d(TAG, "显示授权表单")
        consentForm?.show(
            activity,
            { formError ->
                Log.e(TAG, "授权表单显示错误: ${formError?.message}")
                onConsentFormDismissed()
            }
        ) ?: run {
            Log.w(TAG, "授权表单未加载，尝试重新加载")
            loadConsentForm(activity)
            onConsentFormDismissed()
        }
    }
    
    /**
     * 打开隐私选项表单
     */
    fun showPrivacyOptionsForm(activity: Activity, onPrivacyOptionsFormDismissed: () -> Unit) {
        Log.d(TAG, "打开隐私选项表单")
        UserMessagingPlatform.showPrivacyOptionsForm(
            activity,
            { formError ->
                if (formError != null) {
                    Log.e(TAG, "隐私选项表单显示错误: ${formError.message}")
                } else {
                    Log.d(TAG, "隐私选项表单已关闭")
                }
                onPrivacyOptionsFormDismissed()
            }
        )
    }
}

