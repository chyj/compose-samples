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

package com.example.compose.jetchat

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm.OnConsentFormDismissedListener
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

/**
 * The Google Mobile Ads SDK provides the User Messaging Platform (Google's IAB Certified consent
 * management platform) as one solution to capture consent for users in GDPR impacted countries.
 * This is an example and you can choose another consent management platform to capture consent.
 */
class GoogleMobileAdsConsentManager private constructor(context: Context) {
  private val consentInformation: ConsentInformation =
    UserMessagingPlatform.getConsentInformation(context)

  /** Interface definition for a callback to be invoked when consent gathering is complete. */
  fun interface OnConsentGatheringCompleteListener {
    fun consentGatheringComplete(error: FormError?)
  }

  /** Helper variable to determine if the app can request ads. */
  val canRequestAds: Boolean
    get() {
      val result = consentInformation.canRequestAds()
      Log.v(LOG_TAG, "[canRequestAds] Result: $result")
      return result
    }

  /** Helper variable to determine if the privacy options form is required. */
  val isPrivacyOptionsRequired: Boolean
    get() {
      val result = consentInformation.privacyOptionsRequirementStatus ==
        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
      Log.v(LOG_TAG, "[isPrivacyOptionsRequired] Result: $result")
      return result
    }

  /**
   * Helper method to call the UMP SDK methods to request consent information and load/show a
   * consent form if necessary.
   */
  fun gatherConsent(
    activity: Activity,
    onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener,
  ) {
    Log.i(LOG_TAG, "[gatherConsent] Starting consent gathering process")
    
    // For testing purposes, you can force a DebugGeography of EEA or NOT_EEA.
    val debugSettings =
      ConsentDebugSettings.Builder(activity)
        // .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
        .addTestDeviceHashedId(MyApplication.TEST_DEVICE_HASHED_ID)
        .build()
    Log.d(LOG_TAG, "[gatherConsent] Test device ID: ${MyApplication.TEST_DEVICE_HASHED_ID}")

    val params = ConsentRequestParameters.Builder().setConsentDebugSettings(debugSettings).build()

    // Requesting an update to consent information should be called on every app launch.
    Log.d(LOG_TAG, "[gatherConsent] Requesting consent info update")
    consentInformation.requestConsentInfoUpdate(
      activity,
      params,
      {
        Log.i(LOG_TAG, "[gatherConsent] ✓ Consent info update successful")
        Log.d(LOG_TAG, "[gatherConsent] Loading consent form if required")
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
          if (formError != null) {
            Log.e(LOG_TAG, "[gatherConsent] ✗ Consent form error")
            Log.e(LOG_TAG, "[gatherConsent] Error Code: ${formError.errorCode}")
            Log.e(LOG_TAG, "[gatherConsent] Error Message: ${formError.message}")
          } else {
            Log.i(LOG_TAG, "[gatherConsent] ✓ Consent form completed")
          }
          // Consent has been gathered.
          onConsentGatheringCompleteListener.consentGatheringComplete(formError)
        }
      },
      { requestConsentError ->
        Log.e(LOG_TAG, "[gatherConsent] ✗ Consent info update failed")
        Log.e(LOG_TAG, "[gatherConsent] Error Code: ${requestConsentError.errorCode}")
        Log.e(LOG_TAG, "[gatherConsent] Error Message: ${requestConsentError.message}")
        onConsentGatheringCompleteListener.consentGatheringComplete(requestConsentError)
      },
    )
  }

  /** Helper method to call the UMP SDK method to show the privacy options form. */
  fun showPrivacyOptionsForm(
    activity: Activity,
    onConsentFormDismissedListener: OnConsentFormDismissedListener,
  ) {
    Log.i(LOG_TAG, "[showPrivacyOptionsForm] Showing privacy options form")
    UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
      if (formError != null) {
        Log.e(LOG_TAG, "[showPrivacyOptionsForm] ✗ Form error")
        Log.e(LOG_TAG, "[showPrivacyOptionsForm] Error Code: ${formError.errorCode}")
        Log.e(LOG_TAG, "[showPrivacyOptionsForm] Error Message: ${formError.message}")
      } else {
        Log.i(LOG_TAG, "[showPrivacyOptionsForm] ✓ Form dismissed")
      }
      onConsentFormDismissedListener.onConsentFormDismissed(formError)
    }
  }

  companion object {
    const val LOG_TAG = "AppOpenAd"
    @Volatile private var instance: GoogleMobileAdsConsentManager? = null

    fun getInstance(context: Context) =
      instance
        ?: synchronized(this) {
          instance ?: GoogleMobileAdsConsentManager(context).also { instance = it }
        }
  }
}
