/*
 * Copyright 2025 The Android Open Source Project
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
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 管理 Google Mobile Ads 合规授权流程
 */
class GoogleMobileAdsConsentManager private constructor(private val context: Context) {
    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)
    private var consentForm: ConsentForm? = null

    /**
     * 是否可以请求广告
     */
    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    /**
     * 初始化合规流程
     * @param activity 用于显示授权表单的 Activity
     * @param isDebug 是否为调试模式
     * @param testDeviceIds 测试设备 ID 列表（仅在调试模式下有效）
     */
    suspend fun requestConsentInfoUpdate(
        activity: Activity,
        isDebug: Boolean = false,
        testDeviceIds: List<String> = emptyList(),
    ) {
        try {
            val debugSettings = if (isDebug && testDeviceIds.isNotEmpty()) {
                ConsentDebugSettings.Builder(context)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .addTestDeviceHashedId(testDeviceIds.first())
                    .build()
            } else {
                null
            }

            val params = ConsentRequestParameters.Builder()
                .apply {
                    debugSettings?.let { setConsentDebugSettings(it) }
                }
                .build()

            // requestConsentInfoUpdate 使用回调方式，需要转换为 suspend 函数
            requestConsentInfoUpdateSuspend(activity, params)
            Log.d(TAG, "合规信息更新完成")

            if (consentInformation.isConsentFormAvailable) {
                loadConsentForm(activity)
            }
        } catch (e: Exception) {
            Log.e(TAG, "请求合规信息更新失败", e)
        }
    }

    /**
     * 加载合规表单
     */
    private suspend fun loadConsentForm(activity: Activity) {
        try {
            UserMessagingPlatform.loadConsentForm(
                activity,
                { form ->
                    consentForm = form
                    Log.d(TAG, "合规表单加载成功")
                    // 检查是否需要显示合规表单
                    if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                        showConsentForm(activity)
                    }
                },
                { formError ->
                    Log.e(
                        TAG,
                        "合规表单加载失败: ${formError?.message ?: "未知错误"}, " +
                            "错误码: ${formError?.errorCode ?: -1}",
                    )
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "加载合规表单异常", e)
        }
    }

    /**
     * 显示合规表单
     */
    private fun showConsentForm(activity: Activity) {
        consentForm?.show(
            activity,
            { formError ->
                Log.e(
                    TAG,
                    "显示合规表单失败: ${formError?.message ?: "未知错误"}, " +
                        "错误码: ${formError?.errorCode ?: -1}",
                )
            },
        )
    }

    /**
     * 打开隐私选项表单（用户可随时打开）
     */
    fun showPrivacyOptionsForm(activity: Activity) {
        if (consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        ) {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
                if (formError != null) {
                    Log.e(
                        TAG,
                        "显示隐私选项表单失败: ${formError.message}, " +
                            "错误码: ${formError.errorCode}",
                    )
                } else {
                    Log.d(TAG, "隐私选项表单显示完成")
                }
            }
        } else {
            Log.d(TAG, "当前不需要显示隐私选项表单")
        }
    }

    /**
     * 重置授权状态（仅用于测试）
     */
    fun resetConsent() {
        consentInformation.reset()
        consentForm = null
        Log.d(TAG, "授权状态已重置")
    }

    /**
     * 将 requestConsentInfoUpdate 的回调方式转换为 suspend 函数
     */
    private suspend fun requestConsentInfoUpdateSuspend(
        activity: Activity,
        params: ConsentRequestParameters,
    ) = suspendCancellableCoroutine<Unit> { continuation ->
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // 成功回调
                continuation.resume(Unit)
            },
            { formError ->
                // 失败回调
                val exception = Exception(
                    "请求合规信息更新失败: ${formError?.message ?: "未知错误"}, " +
                        "错误码: ${formError?.errorCode ?: -1}",
                )
                continuation.resumeWithException(exception)
            },
        )
        continuation.invokeOnCancellation {
            // 取消处理
        }
    }

    companion object {
        private const val TAG = "ConsentManager"

        @Volatile
        private var INSTANCE: GoogleMobileAdsConsentManager? = null

        fun getInstance(context: Context): GoogleMobileAdsConsentManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GoogleMobileAdsConsentManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}

