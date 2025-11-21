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
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback

/**
 * Prefetches App Open Ads.
 */
class AppOpenAdManager(private val myApplication: Application) : DefaultLifecycleObserver {
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false

    /** Keep track of the time an app open ad is loaded to ensure you don't show an expired ad. */
    private var loadTime: Long = 0

    companion object {
        private const val LOG_TAG = "OpenAdLifecycle"
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"
        private const val TAG = "AppOpenAdManager"
        /** We will load the ad when the app has been in the foreground for this many seconds. */
        private const val LOAD_TIMEOUT_SECONDS = 4L
    }

    init {
        myApplication.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityStarted(activity: Activity) {
                    // Updating the currentActivity only when an ad is not showing.
                    if (!isShowingAd) {
                        currentActivity = activity
                    }
                }

                override fun onActivityResumed(activity: Activity) {
                    // Updating the currentActivity only when an ad is not showing.
                    if (!isShowingAd) {
                        currentActivity = activity
                    }
                }

                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {}
            }
        )
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /** LifecycleObserver methods */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        showAdIfAvailable()
        Log.d(LOG_TAG, "onStart")
    }

    /** Request an ad */
    fun loadAd(onLoadComplete: ((Boolean) -> Unit)? = null) {
        // Do not load ad if there is an unused ad or one is already loading.
        if (isLoadingAd || isAdAvailable()) {
            Log.d(LOG_TAG, "loadAd: Ad already available or loading")
            onLoadComplete?.invoke(isAdAvailable())
            return
        }
        isLoadingAd = true
        val request = AdRequest.Builder().build()
        Log.d(LOG_TAG, "loadAd: Starting to load ad")
        AppOpenAd.load(
            myApplication,
            AD_UNIT_ID,
            request,
            object : AppOpenAdLoadCallback() {
                /**
                 * Called when an app open ad has loaded.
                 *
                 * @param ad the loaded app open ad.
                 */
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = System.currentTimeMillis()
                    Log.d(LOG_TAG, "loadAd: Ad loaded successfully")
                    logTestDeviceId(ad)
                    onLoadComplete?.invoke(true)
                }

                /**
                 * Called when an app open ad has failed to load.
                 *
                 * @param loadAdError the error.
                 */
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    Log.d(LOG_TAG, "loadAd: Ad failed to load - ${loadAdError.message}")
                    onLoadComplete?.invoke(false)
                }
            }
        )
    }

    /** Check if ad was loaded more than n hours ago. */
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = System.currentTimeMillis() - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    /** Check if ad exists and can be shown. */
    fun isAdAvailable(): Boolean {
        // Ad references in the app open beta will time out after four hours, but this time limit
        // may change in future beta versions. For details, see:
        // https://support.google.com/admob/answer/9341964?hl=en
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    private var currentActivity: Activity? = null

    /** Shows the ad if one isn't already showing. */
    fun showAdIfAvailable(activity: Activity, onAdDismissed: (() -> Unit)? = null) {
        // If the app open ad is already showing, do not show the ad again.
        if (isShowingAd) {
            Log.d(LOG_TAG, "showAdIfAvailable: Ad is already showing")
            return
        }

        // If the app open ad is not available yet, invoke the callback then load the ad.
        if (!isAdAvailable()) {
            Log.d(LOG_TAG, "showAdIfAvailable: Ad not available, loading")
            loadAd()
            return
        }

        Log.d(LOG_TAG, "showAdIfAvailable: Showing ad")
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            /** Called when full screen content is dismissed. */
            override fun onAdDismissedFullScreenContent() {
                // Set the reference to null so isAdAvailable() returns false.
                appOpenAd = null
                isShowingAd = false
                Log.d(LOG_TAG, "onAdDismissedFullScreenContent: Ad dismissed")
                onAdDismissed?.invoke()
                loadAd()
            }

            /** Called when fullscreen content failed to show. */
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                Log.d(LOG_TAG, "onAdFailedToShowFullScreenContent: Ad failed to show - ${adError.message}")
                onAdDismissed?.invoke()
                loadAd()
            }

            /** Called when fullscreen content is shown. */
            override fun onAdShowedFullScreenContent() {
                Log.d(LOG_TAG, "onAdShowedFullScreenContent: Ad showed successfully")
            }
        }

        isShowingAd = true
        appOpenAd?.show(activity)
    }

    fun showAdIfAvailable() {
        // We wrap the showAdIfAvailable to make sure that currentActivity is an instance of
        // Activity and not an AppCompatActivity or any other subclass of Activity.
        currentActivity?.let {
            showAdIfAvailable(it)
        }
    }

    private fun logTestDeviceId(ad: AppOpenAd) {
        val responseInfo = ad.responseInfo
        Log.d(LOG_TAG, "logTestDeviceId: Response Info - ${responseInfo?.responseId}")
        Log.d(LOG_TAG, "logTestDeviceId: Mediation Adapter Class Name - ${responseInfo?.loadedAdapterResponseInfo?.adapterClassName}")
    }
}

