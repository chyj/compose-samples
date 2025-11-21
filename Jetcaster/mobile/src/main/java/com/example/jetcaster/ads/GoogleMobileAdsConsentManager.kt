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

package com.example.jetcaster.ads

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * 管理 Google Mobile Ads 用户同意状态
 * 使用 UMP SDK 处理 GDPR 等隐私合规要求
 */
class GoogleMobileAdsConsentManager private constructor() {
    private var consentInformation: ConsentInformation? = null
    private var consentForm: ConsentForm? = null

    companion object {
        private const val TAG = "ConsentManager"
        private const val TEST_DEBUG_GEOGRAPHY_EEA = "EEA"
        private const val TEST_DEBUG_GEOGRAPHY_NOT_EEA = "NOT_EEA"

        @Volatile
        private var instance: GoogleMobileAdsConsentManager? = null

        fun getInstance(): GoogleMobileAdsConsentManager {
            return instance ?: synchronized(this) {
                instance ?: GoogleMobileAdsConsentManager().also { instance = it }
            }
        }
    }

    /**
     * 初始化同意信息
     * @param activity 当前 Activity
     * @param isDebug 是否为调试模式（调试模式下可以设置测试地理位置）
     */
    fun initialize(activity: Activity, isDebug: Boolean = false) {
        val debugSettings = ConsentDebugSettings.Builder(activity)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId(getTestDeviceHashedId(activity))
            .build()

        val requestParameters = ConsentRequestParameters.Builder()
            .apply {
                if (isDebug) {
                    setConsentDebugSettings(debugSettings)
                }
            }
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation?.requestConsentInfoUpdate(
            activity,
            requestParameters,
            {
                // 同意信息更新成功
                Log.d(TAG, "Consent info updated successfully")
            },
            { formError ->
                // 同意信息更新失败
                Log.e(TAG, "Failed to request consent info update: ${formError.message}")
            }
        )
    }

    /**
     * 检查是否需要显示同意表单
     */
    fun isConsentFormAvailable(): Boolean {
        return consentInformation?.isConsentFormAvailable == true
    }

    /**
     * 检查是否可以加载广告
     */
    fun canRequestAds(): Boolean {
        return consentInformation?.canRequestAds() == true
    }

    /**
     * 加载并显示同意表单
     * @param activity 当前 Activity
     * @param onConsentFormDismissedListener 表单关闭时的回调
     */
    fun loadConsentForm(
        activity: Activity,
        onConsentFormDismissedListener: ConsentForm.OnConsentFormDismissedListener
    ) {
        UserMessagingPlatform.loadConsentForm(
            activity,
            { form ->
                consentForm = form
                if (isConsentFormAvailable()) {
                    form.show(activity, onConsentFormDismissedListener)
                }
            },
            { formError ->
                Log.e(TAG, "Failed to load consent form: ${formError.message}")
            }
        )
    }

    /**
     * 显示隐私选项表单
     * @param activity 当前 Activity
     * @param onConsentFormDismissedListener 表单关闭时的回调
     */
    fun showPrivacyOptionsForm(
        activity: Activity,
        onConsentFormDismissedListener: ConsentForm.OnConsentFormDismissedListener
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(
            activity,
            onConsentFormDismissedListener
        )
    }

    /**
     * 获取测试设备哈希 ID
     */
    private fun getTestDeviceHashedId(activity: Activity): String {
        // 在实际应用中，您应该从 logcat 中获取测试设备的哈希 ID
        // 格式：Use ConsentDebugSettings.Builder().addTestDeviceHashedId("YOUR_TEST_DEVICE_HASHED_ID")
        return ""
    }

    /**
     * 重置同意状态（仅用于测试）
     */
    fun reset() {
        consentInformation?.reset()
    }
}

