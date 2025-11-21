/*
 * Copyright 2020 The Android Open Source Project
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

package com.example.compose.jetchat.ads

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

/**
 * 管理 Google Mobile Ads 同意流程（UMP - User Messaging Platform）
 * 
 * 重要说明：
 * - 仅在 UMP 同意流程成功完成后才允许加载广告
 * - 如需隐私选项，提供入口供用户重新选择
 */
class GoogleMobileAdsConsentManager private constructor() {
    
    private var consentInformation: ConsentInformation? = null
    private var consentForm: ConsentForm? = null
    
    companion object {
        private const val TAG = "ConsentManager"
        
        @Volatile
        private var INSTANCE: GoogleMobileAdsConsentManager? = null
        
        fun getInstance(): GoogleMobileAdsConsentManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GoogleMobileAdsConsentManager().also { INSTANCE = it }
            }
        }
    }
    
    /**
     * 初始化同意信息管理器
     * 
     * @param activity 当前 Activity
     * @param isDebug 是否为调试模式（用于测试同意流程）
     * @param testDeviceId 测试设备 ID（仅在调试模式下使用）
     */
    fun initialize(activity: Activity, isDebug: Boolean = false, testDeviceId: String? = null) {
        val params = ConsentRequestParameters.Builder()
            .apply {
                if (isDebug && testDeviceId != null) {
                    // 调试设置：用于测试同意流程
                    // 使用整数值 1 表示 EEA 地区（欧洲经济区）
                    // 0 = DEBUG_GEOGRAPHY_DISABLED, 1 = DEBUG_GEOGRAPHY_EEA, 2 = DEBUG_GEOGRAPHY_NOT_EEA
                    val debugSettings = ConsentDebugSettings.Builder(activity)
                        .setDebugGeography(1) // EEA 地区
                        .addTestDeviceHashedId(testDeviceId)
                        .build()
                    setConsentDebugSettings(debugSettings)
                }
            }
            .build()
        
        consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation?.requestConsentInfoUpdate(
            activity,
            params,
            {
                // 同意信息更新成功
                Log.d(TAG, "同意信息更新成功")
                if (consentInformation?.isConsentFormAvailable == true) {
                    loadConsentForm(activity)
                }
            },
            { formError ->
                // 同意信息更新失败
                Log.e(TAG, "同意信息更新失败: message=${formError.message}")
            }
        )
    }
    
    /**
     * 加载同意表单
     */
    private fun loadConsentForm(activity: Activity) {
        UserMessagingPlatform.loadConsentForm(
            activity,
            { form ->
                consentForm = form
                if (consentInformation?.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                    form.show(activity) { formError ->
                        // 表单显示完成（用户已做出选择或关闭表单）
                        handleConsentFormResult()
                    }
                }
            },
            { formError ->
                Log.e(TAG, "加载同意表单失败: message=${formError.message}")
            }
        )
    }
    
    /**
     * 处理同意表单结果
     */
    private fun handleConsentFormResult() {
        when (consentInformation?.consentStatus) {
            ConsentInformation.ConsentStatus.OBTAINED -> {
                Log.d(TAG, "用户已同意个性化广告")
            }
            ConsentInformation.ConsentStatus.NOT_REQUIRED -> {
                Log.d(TAG, "不需要显示同意表单")
            }
            ConsentInformation.ConsentStatus.REQUIRED -> {
                Log.d(TAG, "仍需要用户同意")
            }
            else -> {
                Log.d(TAG, "未知的同意状态")
            }
        }
    }
    
    /**
     * 检查是否可以加载广告
     * 
     * @return true 如果可以加载广告，false 否则
     */
    fun canLoadAds(): Boolean {
        return when (consentInformation?.consentStatus) {
            ConsentInformation.ConsentStatus.OBTAINED,
            ConsentInformation.ConsentStatus.NOT_REQUIRED -> true
            else -> false
        }
    }
    
    /**
     * 显示隐私选项表单（供用户重新选择）
     */
    fun showPrivacyOptionsForm(activity: Activity, onComplete: () -> Unit) {
        consentForm?.show(activity) {
            handleConsentFormResult()
            onComplete()
        } ?: run {
            // 如果表单未加载，重新加载
            loadConsentForm(activity)
            onComplete()
        }
    }
    
    /**
     * 重置同意状态（仅用于测试）
     */
    fun resetConsentStatus() {
        consentInformation?.reset()
    }
}

