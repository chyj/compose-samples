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

package com.example.jetcaster

import android.app.Activity
import android.util.Log
import androidx.annotation.NonNull
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

/**
 * Google Mobile Ads Consent manager.
 */
class GoogleMobileAdsConsentManager private constructor() {
    private var consentInformation: ConsentInformation? = null

    companion object {
        private const val LOG_TAG = "OpenAdLifecycle"
        @Volatile
        private var instance: GoogleMobileAdsConsentManager? = null

        fun getInstance(): GoogleMobileAdsConsentManager {
            return instance ?: synchronized(this) {
                instance ?: GoogleMobileAdsConsentManager().also { instance = it }
            }
        }
    }

    fun gatherConsent(
        activity: Activity,
        onConsentGathered: (canRequestAds: Boolean) -> Unit,
        skipConsentDialog: Boolean = true // 测试时跳过同意对话框
    ) {
        // 如果跳过同意对话框，直接返回 true
        if (skipConsentDialog) {
            Log.d(LOG_TAG, "gatherConsent: Skipping consent dialog (test mode)")
            onConsentGathered(true)
            return
        }

        val debugSettings = ConsentDebugSettings.Builder(activity)
            .addTestDeviceHashedId("TEST-DEVICE-HASHED-ID")
            .build()

        val request = ConsentRequestParameters
            .Builder()
            .setConsentDebugSettings(debugSettings)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation!!.requestConsentInfoUpdate(
            activity,
            request,
            {
                Log.d(LOG_TAG, "gatherConsent: UMP consent gathering started")
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    activity
                ) { formError ->
                    // Consent gathering failed.
                    if (formError != null) {
                        Log.d(LOG_TAG, "gatherConsent: UMP consent gathering failed - ${formError.message}")
                        onConsentGathered(false)
                        return@loadAndShowConsentFormIfRequired
                    }

                    // Consent has been gathered.
                    if (consentInformation!!.canRequestAds()) {
                        Log.d(LOG_TAG, "gatherConsent: UMP consent gathering completed, can request ads")
                        onConsentGathered(true)
                    } else {
                        Log.d(LOG_TAG, "gatherConsent: UMP consent gathering completed, cannot request ads")
                        onConsentGathered(false)
                    }
                }
            },
            { requestConsentError ->
                // Consent gathering failed.
                Log.d(LOG_TAG, "gatherConsent: UMP consent info update failed - ${requestConsentError.message}")
                onConsentGathered(false)
            }
        )

        // Check if you can initialize the Google Mobile Ads SDK in parallel
        // while checking for consent.
        if (consentInformation!!.canRequestAds()) {
            Log.d(LOG_TAG, "gatherConsent: Can request ads, initializing SDK")
            onConsentGathered(true)
        }
    }

    fun canRequestAds(): Boolean {
        return consentInformation?.canRequestAds() ?: false
    }

    fun privacyOptionsRequirementStatus(): ConsentInformation.PrivacyOptionsRequirementStatus {
        return consentInformation?.privacyOptionsRequirementStatus
            ?: ConsentInformation.PrivacyOptionsRequirementStatus.UNKNOWN
    }

    fun showPrivacyOptionsForm(
        @NonNull activity: Activity,
        onConsentFormDismissedListener: ConsentForm.OnConsentFormDismissedListener
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, onConsentFormDismissedListener)
    }
}

