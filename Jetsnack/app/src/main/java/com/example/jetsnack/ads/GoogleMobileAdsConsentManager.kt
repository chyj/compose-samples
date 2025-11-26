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

package com.example.jetsnack.ads

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

/**
 * 管理 Google Mobile Ads 用户同意（UMP SDK）的单例类
 */
object GoogleMobileAdsConsentManager {
    private const val TAG = "ConsentManager"

    private var consentForm: ConsentForm? = null

    /**
     * 检查同意状态
     */
    fun canRequestAds(consentInformation: ConsentInformation): Boolean {
        val status = consentInformation.consentStatus
        return status == ConsentInformation.ConsentStatus.OBTAINED ||
            status == ConsentInformation.ConsentStatus.NOT_REQUIRED
    }

    /**
     * 收集同意信息
     */
    fun collectConsent(
        activity: Activity,
        onConsentFormLoadSuccess: () -> Unit,
        onConsentFormLoadFailure: (String) -> Unit,
    ) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadConsentForm(
                    activity,
                    { form ->
                        consentForm = form
                        if (canRequestAds(consentInformation)) {
                            onConsentFormLoadSuccess()
                        } else {
                            showPrivacyOptionsForm(activity, onConsentFormLoadSuccess, onConsentFormLoadFailure)
                        }
                    },
                    { formError: FormError ->
                        Log.e(TAG, "加载同意表单失败: ${formError.message}")
                        onConsentFormLoadFailure(formError.message)
                    },
                )
            },
            { requestConsentError: FormError ->
                Log.e(TAG, "请求同意信息更新失败: ${requestConsentError.message}")
                onConsentFormLoadFailure(requestConsentError.message)
            },
        )
    }

    /**
     * 显示隐私选项表单
     */
    private fun showPrivacyOptionsForm(
        activity: Activity,
        onConsentFormLoadSuccess: () -> Unit,
        onConsentFormLoadFailure: (String) -> Unit,
    ) {
        consentForm?.show(
            activity,
            object : ConsentForm.OnConsentFormDismissedListener {
                override fun onConsentFormDismissed(formError: FormError?) {
                    if (formError != null) {
                        Log.e(TAG, "显示同意表单失败: ${formError.message}")
                        onConsentFormLoadFailure(formError.message)
                    } else {
                        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
                        if (canRequestAds(consentInformation)) {
                            onConsentFormLoadSuccess()
                        } else {
                            onConsentFormLoadFailure("用户未同意")
                        }
                    }
                }
            }
        )
    }

    /**
     * 重置同意状态（用于测试）
     */
    fun resetConsent(activity: Activity) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation.reset()
    }
}

