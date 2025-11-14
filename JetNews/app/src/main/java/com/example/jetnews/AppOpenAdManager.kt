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

package com.example.jetnews

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

/**
 * Prefetches App Open Ads.
 */
class AppOpenAdManager(private val myApplication: MyApplication) : LifecycleObserver,
    Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false

    /** Keep track of the time an app open ad is loaded to ensure you don't show an expired ad. */
    private var loadTime: Long = 0

    /**
     * Load an app open ad.
     *
     * @param context the context of the activity that loads the ad
     */
    fun loadAd(context: Context) {
        Log.d(LOG_TAG, "loadAd() - isLoadingAd: $isLoadingAd, isAdAvailable: ${isAdAvailable()}")
        
        // Do not load ad if there is an unused ad or one is already loading.
        if (isLoadingAd) {
            Log.d(LOG_TAG, "loadAd() - Already loading, skipping")
            return
        }
        
        if (isAdAvailable()) {
            Log.d(LOG_TAG, "loadAd() - Ad already available, skipping")
            return
        }
        
        // Validate Ad Unit ID format
        if (!isValidAdUnitId(AD_UNIT_ID)) {
            Log.e(LOG_TAG, "loadAd() - Invalid Ad Unit ID format: $AD_UNIT_ID")
            Log.e(LOG_TAG, "loadAd() - Expected format: ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX")
            isLoadingAd = false
            return
        }
        
        Log.d(LOG_TAG, "loadAd() - Starting to load ad with AD_UNIT_ID: $AD_UNIT_ID")
        Log.d(LOG_TAG, "loadAd() - Ad Unit ID format validation: PASSED")
        isLoadingAd = true
        
        try {
            val request = AdRequest.Builder().build()
            Log.d(LOG_TAG, "loadAd() - AdRequest built, calling AppOpenAd.load()")
            AppOpenAd.load(
                context,
                AD_UNIT_ID,
                request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    /**
                     * Called when an app open ad has loaded.
                     *
                     * @param ad the loaded app open ad.
                     */
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        isLoadingAd = false
                        loadTime = Date().time
                        Log.d(LOG_TAG, "onAdLoaded() - Success! Load time: $loadTime")
                    }

                    /**
                     * Called when an app open ad has failed to load.
                     *
                     * @param loadAdError the error.
                     */
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isLoadingAd = false
                        Log.e(LOG_TAG, "onAdFailedToLoad() - Error Code: ${loadAdError.code}, " +
                                "Domain: ${loadAdError.domain}, Message: ${loadAdError.message}")
                        Log.e(LOG_TAG, "onAdFailedToLoad() - Response Info: ${loadAdError.responseInfo}")
                        
                        // Detailed error information
                        when (loadAdError.code) {
                            AdRequest.ERROR_CODE_INTERNAL_ERROR -> {
                                Log.e(LOG_TAG, "ERROR_CODE_INTERNAL_ERROR: Something happened internally; for instance, an invalid response was received from the ad server.")
                            }
                            AdRequest.ERROR_CODE_INVALID_REQUEST -> {
                                Log.e(LOG_TAG, "ERROR_CODE_INVALID_REQUEST: The ad request was invalid; for instance, the ad unit ID was incorrect.")
                                Log.e(LOG_TAG, "ERROR_CODE_INVALID_REQUEST: Check if AD_UNIT_ID is correct and is an App Open Ad Unit ID")
                            }
                            AdRequest.ERROR_CODE_NETWORK_ERROR -> {
                                Log.e(LOG_TAG, "ERROR_CODE_NETWORK_ERROR: The ad request was unsuccessful due to network connectivity.")
                            }
                            AdRequest.ERROR_CODE_NO_FILL -> {
                                Log.e(LOG_TAG, "ERROR_CODE_NO_FILL: The ad request was successful, but no ad was returned due to lack of ad inventory.")
                            }
                            3 -> {
                                Log.e(LOG_TAG, "ERROR_CODE_3: Ad unit doesn't match format")
                                Log.e(LOG_TAG, "ERROR_CODE_3: This usually means:")
                                Log.e(LOG_TAG, "  1. The Ad Unit ID is not an App Open Ad type")
                                Log.e(LOG_TAG, "  2. The Ad Unit ID format is incorrect")
                                Log.e(LOG_TAG, "  3. The Ad Unit ID doesn't exist in your AdMob account")
                                Log.e(LOG_TAG, "ERROR_CODE_3: Current AD_UNIT_ID: $AD_UNIT_ID")
                                Log.e(LOG_TAG, "ERROR_CODE_3: Please verify in AdMob console that this is an App Open Ad Unit ID")
                            }
                            else -> {
                                Log.e(LOG_TAG, "Unknown error code: ${loadAdError.code}")
                            }
                        }
                    }
                }
            )
        } catch (e: Exception) {
            isLoadingAd = false
            Log.e(LOG_TAG, "loadAd() - Exception occurred", e)
        }
    }

    /** Check if ad was loaded more than n hours ago. */
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    /** Check if ad exists and can be shown. */
    private fun isAdAvailable(): Boolean {
        // Ad references in the app open beta will time out after 4 hours, but this time limit
        // may change in future beta versions. For details, see:
        // https://support.google.com/admob/answer/9341964?hl=en
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    /**
     * Show the ad if one isn't already showing.
     *
     * @param activity the activity that shows the app open ad
     * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
     */
    fun showAdIfAvailable(
        activity: Activity,
        onShowAdCompleteListener: MyApplication.OnShowAdCompleteListener
    ) {
        // If the app open ad is already showing, do not show the ad again.
        if (isShowingAd) {
            Log.d(LOG_TAG, "The app open ad is already showing.")
            return
        }

        // If the app open ad is not available yet, invoke the callback then load the ad.
        if (!isAdAvailable()) {
            Log.d(LOG_TAG, "The app open ad is not ready yet.")
            onShowAdCompleteListener.onShowAdComplete()
            loadAd(activity)
            return
        }

        Log.d(LOG_TAG, "Will show ad.")

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            /** Called when full screen content is dismissed. */
            override fun onAdDismissedFullScreenContent() {
                // Set the reference to null so isAdAvailable() returns false.
                appOpenAd = null
                isShowingAd = false

                Log.d(LOG_TAG, "onAdDismissedFullScreenContent.")
                onShowAdCompleteListener.onShowAdComplete()
                loadAd(activity)
            }

            /** Called when fullscreen content failed to show. */
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false

                Log.d(LOG_TAG, "onAdFailedToShowFullScreenContent: " + adError.message)
                onShowAdCompleteListener.onShowAdComplete()
                loadAd(activity)
            }

            /** Called when fullscreen content is shown. */
            override fun onAdShowedFullScreenContent() {
                Log.d(LOG_TAG, "onAdShowedFullScreenContent.")
            }
        }

        isShowingAd = true
        appOpenAd?.show(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        // Update current activity reference
        myApplication.currentActivity = activity
        // Updating the app open ad when the app is foregrounded.
        if (!isShowingAd) {
            Log.d(LOG_TAG, "onActivityStarted")
            showAdIfAvailable(
                activity,
                object : MyApplication.OnShowAdCompleteListener {
                    override fun onShowAdComplete() {
                        // Empty because the user will go back to the activity that was shown before
                        // the app open ad.
                    }
                }
            )
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        myApplication.currentActivity?.let { activity ->
            myApplication.showAdIfAvailable(
                activity,
                object : MyApplication.OnShowAdCompleteListener {
                    override fun onShowAdComplete() {
                        // Empty because the user will go back to the activity that was shown before
                        // the app open ad.
                    }
                }
            )
        }
    }

    companion object {
        private const val LOG_TAG = "AppOpenAdManager"
        // TODO: Replace with your actual Ad Unit ID from AdMob console
        // Test Ad Unit ID for App Open Ad: ca-app-pub-3940256099942544/3419835294
        // IMPORTANT: This must be an App Open Ad Unit ID created in AdMob console
        // Production: Replace with your actual App Open Ad Unit ID
        // Format: ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"
        
        /**
         * Validate Ad Unit ID format
         */
        private fun isValidAdUnitId(adUnitId: String): Boolean {
            // App Open Ad Unit ID format: ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
            val pattern = Regex("^ca-app-pub-\\d+/\\d+$")
            return pattern.matches(adUnitId)
        }
    }
}

