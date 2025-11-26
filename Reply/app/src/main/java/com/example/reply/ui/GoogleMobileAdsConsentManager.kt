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

package com.example.reply.ui

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

private const val TAG = "OpenAdLifecycle"

class GoogleMobileAdsConsentManager(private val activity: Activity) {

    private var consentForm: ConsentForm? = null
    private var consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)

    fun gatherConsent(
        skipConsentDialog: Boolean = true,
        onConsentGathered: (() -> Unit)? = null
    ) {
        val debugSettings = ConsentDebugSettings.Builder(activity)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId("TEST_DEVICE_ID")
            .build()

        val requestParameters = ConsentRequestParameters.Builder()
            .setConsentDebugSettings(if (skipConsentDialog) debugSettings else null)
            .setTagForUnderAgeOfConsent(false)
            .build()

        Log.d(TAG, "gatherConsent: Requesting consent info...")
        consentInformation.requestConsentInfoUpdate(
            activity,
            requestParameters,
            {
                Log.d(TAG, "gatherConsent: Consent info updated")
                if (skipConsentDialog) {
                    // 测试环境：直接同意
                    Log.d(TAG, "gatherConsent: Skipping consent dialog (test mode)")
                    onConsentGathered?.invoke()
                } else {
                    // 生产环境：显示同意对话框
                    if (consentInformation.isConsentFormAvailable) {
                        loadAndShowConsentForm(onConsentGathered)
                    } else {
                        Log.d(TAG, "gatherConsent: Consent form not available")
                        onConsentGathered?.invoke()
                    }
                }
            },
            { requestConsentError ->
                Log.e(TAG, "gatherConsent: Error requesting consent: ${requestConsentError.message}")
                onConsentGathered?.invoke()
            }
        )
    }

    private fun loadAndShowConsentForm(onConsentGathered: (() -> Unit)?) {
        Log.d(TAG, "loadAndShowConsentForm: Loading consent form...")
        UserMessagingPlatform.loadConsentForm(
            activity,
            { form ->
                Log.d(TAG, "loadAndShowConsentForm: Consent form loaded")
                consentForm = form
                if (consentInformation.isConsentFormAvailable) {
                    form.show(
                        activity,
                        object : ConsentForm.OnConsentFormDismissedListener {
                            override fun onConsentFormDismissed(error: FormError?) {
                                if (error != null) {
                                    Log.e(TAG, "loadAndShowConsentForm: Error showing form: ${error.message}")
                                } else {
                                    Log.d(TAG, "loadAndShowConsentForm: Consent form dismissed")
                                }
                                onConsentGathered?.invoke()
                            }
                        }
                    )
                } else {
                    Log.d(TAG, "loadAndShowConsentForm: Consent form should not be shown")
                    onConsentGathered?.invoke()
                }
            },
            { formError ->
                Log.e(TAG, "loadAndShowConsentForm: Error loading form: ${formError.message}")
                onConsentGathered?.invoke()
            }
        )
    }

    fun showPrivacyOptionsForm(onFormDismissed: (() -> Unit)? = null) {
        if (consentForm == null) {
            Log.d(TAG, "showPrivacyOptionsForm: Loading privacy options form...")
            UserMessagingPlatform.loadConsentForm(
                activity,
                { form ->
                    Log.d(TAG, "showPrivacyOptionsForm: Privacy options form loaded")
                    consentForm = form
                    if (consentInformation.privacyOptionsRequirementStatus ==
                        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                    ) {
                        form.show(
                            activity,
                            object : ConsentForm.OnConsentFormDismissedListener {
                                override fun onConsentFormDismissed(error: FormError?) {
                                    if (error != null) {
                                        Log.e(TAG, "showPrivacyOptionsForm: Error showing form: ${error.message}")
                                    } else {
                                        Log.d(TAG, "showPrivacyOptionsForm: Privacy options form dismissed")
                                    }
                                    onFormDismissed?.invoke()
                                }
                            }
                        )
                    } else {
                        Log.d(TAG, "showPrivacyOptionsForm: Privacy options form not required")
                        onFormDismissed?.invoke()
                    }
                },
                { formError ->
                    Log.e(TAG, "showPrivacyOptionsForm: Error loading form: ${formError.message}")
                    onFormDismissed?.invoke()
                }
            )
        } else {
            Log.d(TAG, "showPrivacyOptionsForm: Showing existing privacy options form")
            if (consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
            ) {
                consentForm?.show(
                    activity,
                    object : ConsentForm.OnConsentFormDismissedListener {
                        override fun onConsentFormDismissed(error: FormError?) {
                            if (error != null) {
                                Log.e(TAG, "showPrivacyOptionsForm: Error showing form: ${error.message}")
                            } else {
                                Log.d(TAG, "showPrivacyOptionsForm: Privacy options form dismissed")
                            }
                            onFormDismissed?.invoke()
                        }
                    }
                )
            } else {
                Log.d(TAG, "showPrivacyOptionsForm: Privacy options form not required")
                onFormDismissed?.invoke()
            }
        }
    }

    fun canRequestAds(): Boolean {
        return consentInformation.canRequestAds()
    }
}

