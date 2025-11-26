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
import java.util.Date

private const val TAG = "OpenAdLifecycle"
private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"

class AppOpenAdManager(private val application: Application) : DefaultLifecycleObserver {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Long = 0
    private var currentActivity: Activity? = null
    private var isSplashScreenActive = false // 标记 SplashActivity 是否活跃
    private var pendingLoadCallbacks = mutableListOf<((Boolean) -> Unit)?>() // 等待加载完成的回调列表

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d(TAG, "onStart")
        // 如果 SplashActivity 活跃，不自动显示广告
        if (!isShowingAd && !isSplashScreenActive) {
            showAdIfAvailable()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d(TAG, "onStop")
    }

    fun loadAd(callback: ((Boolean) -> Unit)? = null) {
        if (isAdAvailable()) {
            Log.d(TAG, "loadAd: Ad already available")
            callback?.invoke(true)
            return
        }

        // 如果正在加载，将回调添加到等待列表
        if (isLoadingAd) {
            Log.d(TAG, "loadAd: Ad already loading, adding callback to pending list")
            if (callback != null) {
                pendingLoadCallbacks.add(callback)
            }
            return
        }

        isLoadingAd = true
        Log.d(TAG, "loadAd: Loading ad...")

        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            application,
            AD_UNIT_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "loadAd: Ad loaded")
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    callback?.invoke(true)
                    // 通知所有等待的回调
                    pendingLoadCallbacks.forEach { it?.invoke(true) }
                    pendingLoadCallbacks.clear()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "loadAd: Ad failed to load: ${loadAdError.message}")
                    isLoadingAd = false
                    callback?.invoke(false)
                    // 通知所有等待的回调
                    pendingLoadCallbacks.forEach { it?.invoke(false) }
                    pendingLoadCallbacks.clear()
                }
            }
        )
    }

    fun showAdIfAvailable(activity: Activity? = null, onAdDismissed: (() -> Unit)? = null) {
        if (isShowingAd) {
            Log.d(TAG, "showAdIfAvailable: Ad already showing")
            return
        }

        if (!isAdAvailable()) {
            Log.d(TAG, "showAdIfAvailable: Ad not available")
            onAdDismissed?.invoke()
            loadAd()
            return
        }

        if (activity == null && currentActivity == null) {
            Log.d(TAG, "showAdIfAvailable: No activity available")
            onAdDismissed?.invoke()
            return
        }

        val activityToUse = activity ?: currentActivity ?: return
        isShowingAd = true

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "showAdIfAvailable: Ad dismissed")
                appOpenAd = null
                isShowingAd = false
                onAdDismissed?.invoke()
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "showAdIfAvailable: Ad failed to show: ${adError.message}")
                appOpenAd = null
                isShowingAd = false
                onAdDismissed?.invoke()
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "showAdIfAvailable: Ad showed")
            }
        }

        appOpenAd?.show(activityToUse)
    }

    fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    fun setCurrentActivity(activity: Activity?) {
        currentActivity = activity
    }

    fun setSplashScreenActive(active: Boolean) {
        isSplashScreenActive = active
        Log.d(TAG, "setSplashScreenActive: $active")
    }
}

