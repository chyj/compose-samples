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
import android.app.Application
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

/**
 * Manager class for App Open Ads.
 * This class handles loading and showing app open ads.
 */
class AppOpenAdManager(private val application: Application) {
  private var appOpenAd: AppOpenAd? = null
  private var isLoadingAd = false
  var isShowingAd = false
    private set
  private var loadTime: Long = 0
  private var pendingActivity: Activity? = null
  private var pendingCallback: OnShowAdCompleteListener? = null

  /**
   * Interface definition for a callback to be invoked when an ad is shown.
   */
  interface OnShowAdCompleteListener {
    fun onShowAdComplete()
  }

  /**
   * Loads an app open ad.
   */
  fun loadAd(activity: Activity) {
    // Do not load ad if there is an unused ad or one is already loading.
    if (isLoadingAd || isAdAvailable()) {
      Log.d(LOG_TAG, "[loadAd] Ad already loading or available, skipping")
      Log.d(LOG_TAG, "[loadAd] isLoadingAd: $isLoadingAd, isAdAvailable: ${isAdAvailable()}")
      return
    }

    isLoadingAd = true
    Log.i(LOG_TAG, "[loadAd] Starting to load app open ad")
    Log.d(LOG_TAG, "[loadAd] Using Ad Unit ID: $AD_UNIT_ID")
    Log.d(LOG_TAG, "[loadAd] Application context: ${application.javaClass.simpleName}")
    Log.d(LOG_TAG, "[loadAd] Activity: ${activity.javaClass.simpleName}")
    
    val request = AdRequest.Builder().build()
    Log.d(LOG_TAG, "[loadAd] AdRequest created, calling AppOpenAd.load()")
    Log.d(LOG_TAG, "[loadAd] Using SDK 24.4.0+ API (no orientation parameter)")
    
    // SDK 24.4.0+ API: AppOpenAd.load(Context, String, AdRequest, AppOpenAdLoadCallback)
    AppOpenAd.load(
      application,
      AD_UNIT_ID,
      request,
      object : AppOpenAd.AppOpenAdLoadCallback() {
        override fun onAdLoaded(ad: AppOpenAd) {
          Log.i(LOG_TAG, "[loadAd] ✓ Ad loaded successfully")
          Log.d(LOG_TAG, "[loadAd] Ad response info: ${ad.responseInfo}")
          appOpenAd = ad
          isLoadingAd = false
          loadTime = Date().time
          Log.d(LOG_TAG, "[loadAd] Load time set to: $loadTime")
          
          // If there's a pending activity waiting for the ad, show it now
          pendingActivity?.let { activity ->
            pendingCallback?.let { callback ->
              Log.i(LOG_TAG, "[loadAd] Pending activity found, showing ad immediately")
              pendingActivity = null
              pendingCallback = null
              showAdIfAvailable(activity, callback)
            }
          }
        }

        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
          Log.e(LOG_TAG, "[loadAd] ✗ Ad failed to load")
          Log.e(LOG_TAG, "[loadAd] Error Code: ${loadAdError.code}")
          Log.e(LOG_TAG, "[loadAd] Error Code Name: ${getErrorCodeName(loadAdError.code)}")
          Log.e(LOG_TAG, "[loadAd] Error Message: ${loadAdError.message}")
          Log.e(LOG_TAG, "[loadAd] Error Domain: ${loadAdError.domain}")
          Log.e(LOG_TAG, "[loadAd] Cause: ${loadAdError.cause}")
          Log.e(LOG_TAG, "[loadAd] Response Info: ${loadAdError.responseInfo}")
          Log.e(LOG_TAG, "[loadAd] Ad Unit ID used: $AD_UNIT_ID")
          isLoadingAd = false
          
          // If there's a pending callback, invoke it since ad failed to load
          pendingCallback?.let { callback ->
            Log.w(LOG_TAG, "[loadAd] Ad failed, invoking pending callback")
            pendingActivity = null
            pendingCallback = null
            callback.onShowAdComplete()
          }
        }
      },
    )
  }

  /**
   * Get human-readable error code name for debugging.
   */
  private fun getErrorCodeName(code: Int): String {
    return when (code) {
      0 -> "ERROR_CODE_INTERNAL_ERROR"
      1 -> "ERROR_CODE_INVALID_REQUEST"
      2 -> "ERROR_CODE_NETWORK_ERROR"
      3 -> "ERROR_CODE_NO_FILL"
      8 -> "ERROR_CODE_INVALID_AD_SIZE"
      9 -> "ERROR_CODE_AD_REUSED"
      else -> "UNKNOWN_ERROR_CODE"
    }
  }

  /**
   * Checks if ad was loaded less than n hours ago.
   */
  private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
    val dateDifference = Date().time - loadTime
    val numMilliSecondsPerHour: Long = 3600000
    return dateDifference < numMilliSecondsPerHour * numHours
  }

  /**
   * Checks if ad is available to show.
   */
  fun isAdAvailable(): Boolean {
    return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
  }

  /**
   * Shows the ad if one isn't already showing.
   */
  fun showAdIfAvailable(
    activity: Activity,
    onShowAdCompleteListener: OnShowAdCompleteListener,
  ) {
    // If the app open ad is already showing, do not show the ad again.
    if (isShowingAd) {
      Log.w(LOG_TAG, "[showAdIfAvailable] Ad is already showing, skipping")
      onShowAdCompleteListener.onShowAdComplete()
      return
    }

    // Check if activity is still valid
    if (activity.isFinishing || activity.isDestroyed) {
      Log.w(LOG_TAG, "[showAdIfAvailable] Activity is finishing/destroyed, skipping")
      onShowAdCompleteListener.onShowAdComplete()
      return
    }

    // If the app open ad is not available yet, wait for it to load
    if (!isAdAvailable()) {
      if (isLoadingAd) {
        // Ad is currently loading, save activity and callback to show when ready
        Log.i(LOG_TAG, "[showAdIfAvailable] Ad is loading, saving activity and callback for later")
        pendingActivity = activity
        pendingCallback = onShowAdCompleteListener
        return
      } else {
        // No ad loading, start loading and invoke callback
        Log.w(LOG_TAG, "[showAdIfAvailable] Ad not available, loading new ad")
        onShowAdCompleteListener.onShowAdComplete()
        loadAd(activity)
        return
      }
    }

    // Double check activity is still valid before showing
    if (activity.isFinishing || activity.isDestroyed) {
      Log.w(LOG_TAG, "[showAdIfAvailable] Activity became invalid before showing ad")
      onShowAdCompleteListener.onShowAdComplete()
      return
    }

    Log.i(LOG_TAG, "[showAdIfAvailable] Showing app open ad")
    appOpenAd?.fullScreenContentCallback =
      object : FullScreenContentCallback() {
        override fun onAdDismissedFullScreenContent() {
          // Set the reference to null so isAdAvailable() returns false.
          Log.i(LOG_TAG, "[onAdDismissedFullScreenContent] Ad dismissed")
          appOpenAd = null
          isShowingAd = false
          onShowAdCompleteListener.onShowAdComplete()
          // Only load next ad if activity is still valid
          if (!activity.isFinishing && !activity.isDestroyed) {
            loadAd(activity)
          }
        }

        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
          Log.e(LOG_TAG, "[onAdFailedToShowFullScreenContent] Ad failed to show")
          Log.e(LOG_TAG, "[onAdFailedToShowFullScreenContent] Error Code: ${adError.code}")
          Log.e(LOG_TAG, "[onAdFailedToShowFullScreenContent] Error Message: ${adError.message}")
          appOpenAd = null
          isShowingAd = false
          onShowAdCompleteListener.onShowAdComplete()
          loadAd(activity)
        }

        override fun onAdShowedFullScreenContent() {
          Log.i(LOG_TAG, "[onAdShowedFullScreenContent] Ad showed successfully")
          isShowingAd = true
        }

        override fun onAdImpression() {
          Log.d(LOG_TAG, "[onAdImpression] Ad impression recorded")
        }

        override fun onAdClicked() {
          Log.d(LOG_TAG, "[onAdClicked] Ad clicked")
        }
      }

    isShowingAd = true
    appOpenAd?.show(activity)
  }

  companion object {
    // Use test ad unit ID for App Open Ads.
    // Test ad unit ID format: ca-app-pub-3940256099942544/3419835294
    // ca-app-pub-3940256099942544/9257395921
    // Make sure this is an App Open Ad unit ID in your AdMob account.
    // For debug/testing purposes only.
    private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"
    const val LOG_TAG = "AppOpenAd"
    
    // Debug flag
    private const val DEBUG_MODE = true
  }
}

