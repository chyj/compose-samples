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
import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Google Mobile Ads 合规授权管理器
 * 处理 GDPR/CCPA 等隐私合规要求
 * 
 * 使用 User Messaging Platform (UMP) SDK 进行合规授权管理
 */
@Keep
class GoogleMobileAdsConsentManager private constructor(context: Context) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    // 使用 AtomicBoolean 确保线程安全的访问同意表单状态
    private val isConsentFormShowing = AtomicBoolean(false)
    
    // 存储隐私选项表单
    private var privacyOptionsForm: ConsentForm? = null

    companion object {
        private const val TAG = "AdMobConsent"
        
        @Volatile
        private var INSTANCE: GoogleMobileAdsConsentManager? = null

        fun getInstance(context: Context): GoogleMobileAdsConsentManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GoogleMobileAdsConsentManager(context.applicationContext)
                    .also { INSTANCE = it }
            }
        }
    }

    /**
     * 是否可以请求广告
     */
    val canRequestAds: Boolean
        get() {
            val canRequest = consentInformation.canRequestAds()
            Log.d(TAG, "canRequestAds: $canRequest, consentStatus: ${consentInformation.consentStatus}")
            return canRequest
        }

    /**
     * 是否需要显示隐私选项表单
     */
    val isPrivacyOptionsRequired: Boolean
        get() {
            val isRequired = consentInformation.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
            Log.d(TAG, "isPrivacyOptionsRequired: $isRequired")
            return isRequired
        }

    /**
     * 收集用户同意信息
     * @param activity 用于显示同意表单的 Activity
     * @param testDeviceIds 测试设备 ID 列表（可选）
     * @param isDebugGeography 是否使用调试地理位置（仅用于测试）
     * @param onConsentGathered 同意收集完成后的回调
     */
    fun gatherConsent(
        activity: Activity,
        testDeviceIds: List<String>? = null,
        isDebugGeography: Boolean = false,
        onConsentGathered: (Boolean) -> Unit
    ) {
        Log.d(TAG, "gatherConsent: start, testDeviceIds=$testDeviceIds, isDebugGeography=$isDebugGeography")
        
        val debugSettings = ConsentDebugSettings.Builder(activity)
            .apply {
                if (isDebugGeography) {
                    // 设置为 EEA（欧洲经济区）以测试同意流程
                    setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    Log.d(TAG, "Debug geography set to EEA")
                }
                testDeviceIds?.forEach { deviceId ->
                    addTestDeviceHashedId(deviceId)
                    Log.d(TAG, "Added test device: $deviceId")
                }
            }
            .build()

        val params = ConsentRequestParameters.Builder()
            .setConsentDebugSettings(debugSettings)
            .build()

        // 请求更新同意信息
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                Log.d(TAG, "Consent info update succeeded. Status: ${consentInformation.consentStatus}")
                // 如果需要，加载并显示同意表单
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.e(TAG, "Error showing consent form: ${formError.message}, errorCode: ${formError.errorCode}")
                    } else {
                        Log.d(TAG, "Consent form shown successfully. Status: ${consentInformation.consentStatus}")
                    }
                    isConsentFormShowing.set(false)
                    onConsentGathered(canRequestAds)
                }
            },
            { requestError ->
                Log.e(TAG, "Error requesting consent info update: ${requestError.message}, errorCode: ${requestError.errorCode}")
                isConsentFormShowing.set(false)
                onConsentGathered(canRequestAds)
            }
        )
    }

    /**
     * 显示隐私选项表单（允许用户随时修改同意设置）
     */
    fun showPrivacyOptionsForm(activity: Activity, onFormClosed: () -> Unit = {}) {
        Log.d(TAG, "showPrivacyOptionsForm: start")
        if (isConsentFormShowing.compareAndSet(false, true)) {
            // 如果已有表单，直接显示
            if (privacyOptionsForm != null) {
                privacyOptionsForm?.show(activity) { formError: FormError? ->
                    if (formError != null) {
                        Log.e(TAG, "Error showing privacy options form: ${formError.message ?: "Unknown error"}, errorCode: ${formError.errorCode}")
                    } else {
                        Log.d(TAG, "Privacy options form shown successfully. Status: ${consentInformation.consentStatus}")
                    }
                    isConsentFormShowing.set(false)
                    onFormClosed()
                }
            } else {
                // 加载隐私选项表单 - 使用 loadConsentForm 加载表单
                UserMessagingPlatform.loadConsentForm(
                    activity,
                    { form: ConsentForm ->
                        Log.d(TAG, "Privacy options form loaded successfully")
                        privacyOptionsForm = form
                        form.show(activity) { formError: FormError? ->
                            if (formError != null) {
                                Log.e(TAG, "Error showing privacy options form: ${formError.message ?: "Unknown error"}, errorCode: ${formError.errorCode}")
                            } else {
                                Log.d(TAG, "Privacy options form shown successfully. Status: ${consentInformation.consentStatus}")
                            }
                            isConsentFormShowing.set(false)
                            onFormClosed()
                        }
                    },
                    { formError: FormError ->
                        Log.e(TAG, "Error loading privacy options form: ${formError.message ?: "Unknown error"}, errorCode: ${formError.errorCode}")
                        isConsentFormShowing.set(false)
                        onFormClosed()
                    }
                )
            }
        } else {
            Log.w(TAG, "Privacy options form is already showing")
            onFormClosed()
        }
    }

    /**
     * 重置同意状态（仅用于测试）
     */
    fun resetConsent() {
        Log.d(TAG, "resetConsent: resetting consent information")
        consentInformation.reset()
    }
}

