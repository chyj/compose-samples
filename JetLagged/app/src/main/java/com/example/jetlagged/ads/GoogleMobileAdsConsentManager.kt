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

package com.example.jetlagged.ads

import android.app.Activity
import android.util.Log
import androidx.annotation.MainThread
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "AdMobConsent"

/**
 * Google Mobile Ads Consent Manager
 * 处理用户同意流程，确保符合 GDPR 和其他隐私法规要求
 */
class GoogleMobileAdsConsentManager(private val activity: Activity) {

    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(activity)

    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private var consentForm: ConsentForm? = null

    /**
     * 检查是否可以请求广告
     */
    val canRequestAdsValue: Boolean
        get() = consentInformation.canRequestAds()

    init {
        Log.d(TAG, "GoogleMobileAdsConsentManager initialized")
        val initialCanRequestAds = consentInformation.canRequestAds()
        Log.d(TAG, "Initial canRequestAds value: $initialCanRequestAds")
        _canRequestAds.value = initialCanRequestAds
    }

    /**
     * 收集同意信息
     * @param debugDeviceIds 测试设备 ID 列表（用于调试）
     * @param isDebugGeography 是否使用调试地理位置（EU/EEA）
     */
    @MainThread
    fun collectConsentInfo(
        debugDeviceIds: List<String> = emptyList(),
        isDebugGeography: Boolean = false,
    ) {
        Log.d(TAG, "collectConsentInfo called, debugDeviceIds: $debugDeviceIds, isDebugGeography: $isDebugGeography")

        val params = ConsentRequestParameters.Builder().apply {
            if (debugDeviceIds.isNotEmpty() || isDebugGeography) {
                val debugSettings = ConsentDebugSettings.Builder(activity).apply {
                    debugDeviceIds.forEach { deviceId ->
                        addTestDeviceHashedId(deviceId)
                        Log.d(TAG, "Added test device ID: $deviceId")
                    }
                    if (isDebugGeography) {
                        setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                        Log.d(TAG, "Debug geography set to EEA")
                    }
                }.build()
                setConsentDebugSettings(debugSettings)
            }
        }.build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                Log.d(TAG, "Consent info update succeeded. canRequestAds: ${consentInformation.canRequestAds()}")
                _canRequestAds.value = consentInformation.canRequestAds()

                if (consentInformation.isConsentFormAvailable()) {
                    loadConsentForm()
                } else {
                    Log.d(TAG, "Consent form not available")
                }
            },
            { formError: FormError ->
                Log.e(TAG, "Consent info update failed: ${formError.message}, errorCode: ${formError.errorCode}")
                _canRequestAds.value = consentInformation.canRequestAds()
            },
        )
    }

    /**
     * 加载同意表单
     */
    @MainThread
    private fun loadConsentForm() {
        Log.d(TAG, "Loading consent form")
        UserMessagingPlatform.loadConsentForm(
            activity,
            { form: ConsentForm ->
                Log.d(TAG, "Consent form loaded successfully")
                consentForm = form
                if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                    Log.d(TAG, "Consent status is REQUIRED, showing form")
                    form.show(activity) { formError: FormError? ->
                        if (formError != null) {
                            Log.e(TAG, "Consent form error: ${formError.message}, errorCode: ${formError.errorCode}")
                        } else {
                            Log.d(TAG, "Consent form dismissed")
                        }
                        _canRequestAds.value = consentInformation.canRequestAds()
                    }
                } else {
                    Log.d(TAG, "Consent status: ${consentInformation.consentStatus}, form not required")
                }
            },
            { formError: FormError ->
                Log.e(TAG, "Failed to load consent form: ${formError.message}, errorCode: ${formError.errorCode}")
            },
        )
    }

    /**
     * 打开隐私选项表单（用户可以随时更改同意设置）
     */
    @MainThread
    fun showPrivacyOptionsForm() {
        Log.d(TAG, "showPrivacyOptionsForm called")
        if (consentForm != null) {
            consentForm?.show(activity) { formError: FormError? ->
                if (formError != null) {
                    Log.e(TAG, "Privacy options form error: ${formError.message}, errorCode: ${formError.errorCode}")
                } else {
                    Log.d(TAG, "Privacy options form dismissed")
                }
                _canRequestAds.value = consentInformation.canRequestAds()
            }
        } else {
            Log.w(TAG, "Consent form is null, loading it first")
            loadConsentForm()
        }
    }

    /**
     * 重置同意状态（用于测试）
     */
    fun resetConsent() {
        Log.d(TAG, "Resetting consent")
        consentInformation.reset()
        _canRequestAds.value = false
    }
}
