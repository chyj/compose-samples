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

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Splash Activity that inflates splash activity xml. */
class SplashActivity : AppCompatActivity() {

  private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager
  private val isMobileAdsInitializeCalled = AtomicBoolean(false)
  private val gatherConsentFinished = AtomicBoolean(false)
  private var secondsRemaining: Long = 0L

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Log.i(LOG_TAG, "[onCreate] SplashActivity started")
    setContentView(R.layout.activity_splash)

    // Log the Mobile Ads SDK version.
    val sdkVersion = MobileAds.getVersion()
    Log.i(LOG_TAG, "[onCreate] Google Mobile Ads SDK Version: $sdkVersion")

    // Create a timer so the SplashActivity will be displayed for a fixed amount of time.
    createTimer()

    googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(applicationContext)
    val canRequestAdsBeforeConsent = googleMobileAdsConsentManager.canRequestAds
    Log.d(LOG_TAG, "[onCreate] Can request ads (before consent): $canRequestAdsBeforeConsent")
    
    Log.i(LOG_TAG, "[onCreate] Starting consent gathering")
    googleMobileAdsConsentManager.gatherConsent(this) { consentError ->
      if (consentError != null) {
        // Consent not obtained in current session.
        Log.e(LOG_TAG, "[gatherConsent] ✗ Consent gathering failed")
        Log.e(LOG_TAG, "[gatherConsent] Error Code: ${consentError.errorCode}")
        Log.e(LOG_TAG, "[gatherConsent] Error Message: ${consentError.message}")
      } else {
        Log.i(LOG_TAG, "[gatherConsent] ✓ Consent gathering completed")
      }

      gatherConsentFinished.set(true)
      val canRequestAdsAfterConsent = googleMobileAdsConsentManager.canRequestAds
      Log.d(LOG_TAG, "[gatherConsent] Can request ads (after consent): $canRequestAdsAfterConsent")

      if (canRequestAdsAfterConsent) {
        Log.i(LOG_TAG, "[gatherConsent] Initializing Mobile Ads SDK")
        initializeMobileAdsSdk()
      } else {
        Log.w(LOG_TAG, "[gatherConsent] Cannot initialize SDK - consent not granted")
      }

      if (secondsRemaining <= 0) {
        Log.d(LOG_TAG, "[gatherConsent] Timer finished, starting MainActivity")
        startMainActivity()
      }
    }

    // This sample attempts to load ads using consent obtained in the previous session.
    if (canRequestAdsBeforeConsent) {
      Log.i(LOG_TAG, "[onCreate] Consent already granted, initializing SDK immediately")
      initializeMobileAdsSdk()
    }
  }

  /**
   * Create the countdown timer, which counts down to zero and show the app open ad.
   *
   * @param time the number of milliseconds that the timer counts down from
   */
  private fun createTimer() {
    val counterTextView: TextView = findViewById(R.id.timer)
    val countDownTimer: CountDownTimer =
      object : CountDownTimer(COUNTER_TIME_MILLISECONDS, 1000) {
        override fun onTick(millisUntilFinished: Long) {
          secondsRemaining = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1
          counterTextView.text = "App is done loading in: $secondsRemaining"
        }

        override fun onFinish() {
          secondsRemaining = 0
          counterTextView.text = "Done."
          Log.i(LOG_TAG, "[Timer] Countdown finished, attempting to show ad")

          (application as MyApplication).showAdIfAvailable(
            this@SplashActivity,
            object : AppOpenAdManager.OnShowAdCompleteListener {
              override fun onShowAdComplete() {
                Log.d(LOG_TAG, "[Timer] onShowAdComplete callback invoked")
                // Check if the consent form is currently on screen before moving to the main
                // activity.
                if (gatherConsentFinished.get()) {
                  Log.i(LOG_TAG, "[Timer] Consent finished, starting MainActivity")
                  startMainActivity()
                } else {
                  Log.w(LOG_TAG, "[Timer] Consent not finished yet, waiting...")
                }
              }
            },
          )
        }
      }
    countDownTimer.start()
  }

  private fun initializeMobileAdsSdk() {
    if (isMobileAdsInitializeCalled.getAndSet(true)) {
      Log.w(LOG_TAG, "[initializeMobileAdsSdk] Already initialized, skipping")
      return
    }

    Log.i(LOG_TAG, "[initializeMobileAdsSdk] Starting SDK initialization")
    
    // Set your test devices.
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTestDeviceIds(listOf(MyApplication.TEST_DEVICE_HASHED_ID))
        .build()
    )
    Log.d(LOG_TAG, "[initializeMobileAdsSdk] Test device ID set: ${MyApplication.TEST_DEVICE_HASHED_ID}")

    CoroutineScope(Dispatchers.IO).launch {
      Log.d(LOG_TAG, "[initializeMobileAdsSdk] Initializing SDK on background thread")
      // Initialize the Google Mobile Ads SDK on a background thread.
      MobileAds.initialize(this@SplashActivity) { initializationStatus ->
        Log.i(LOG_TAG, "[initializeMobileAdsSdk] ✓ SDK initialization completed")
        val adapterStatuses = initializationStatus.adapterStatusMap
        adapterStatuses.forEach { (adapter, status) ->
          Log.d(LOG_TAG, "[initializeMobileAdsSdk] Adapter: $adapter, State: ${status.initializationState}, Description: ${status.description}")
        }
      }
      runOnUiThread {
        Log.d(LOG_TAG, "[initializeMobileAdsSdk] Loading ad on main thread")
        // Load an ad on the main thread.
        (application as MyApplication).loadAd(this@SplashActivity)
      }
    }
  }

  /** Start the MainActivity. */
  fun startMainActivity() {
    Log.i(LOG_TAG, "[startMainActivity] Navigating to NavActivity")
    val intent = Intent(this, NavActivity::class.java)
    startActivity(intent)
    finish()
  }

  companion object {
    // Number of milliseconds to count down before showing the app open ad. This simulates the time
    // needed to load the app.
    private const val COUNTER_TIME_MILLISECONDS = 10000L

    const val LOG_TAG = "AppOpenAd"
  }
}
