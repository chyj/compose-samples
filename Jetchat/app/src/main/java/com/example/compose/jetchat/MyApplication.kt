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
import android.os.Bundle
import android.util.Log

/**
 * Application class that manages app-level lifecycle and app open ads.
 */
class MyApplication : Application() {
  private lateinit var appOpenAdManager: AppOpenAdManager
  private var currentActivity: Activity? = null

  companion object {
    // Test device hashed ID for AdMob testing
    // Replace with your actual test device ID
    const val TEST_DEVICE_HASHED_ID = "33BE2250B43518CCDA7DE426D04EE231"
    const val LOG_TAG = "AppOpenAd"
  }

  override fun onCreate() {
    super.onCreate()
    Log.i(LOG_TAG, "[MyApplication.onCreate] Application started")
    
    appOpenAdManager = AppOpenAdManager(this)
    
    registerActivityLifecycleCallbacks(
      object : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
          Log.d(LOG_TAG, "[onActivityCreated] Activity: ${activity.javaClass.simpleName}")
        }

        override fun onActivityStarted(activity: Activity) {
          Log.d(LOG_TAG, "[onActivityStarted] Activity: ${activity.javaClass.simpleName}")
          currentActivity = activity
          
          // Don't show app open ad if it's already showing or if it's SplashActivity
          if (!appOpenAdManager.isShowingAd && activity !is SplashActivity) {
            Log.d(LOG_TAG, "[onActivityStarted] Attempting to show app open ad")
            appOpenAdManager.showAdIfAvailable(
              activity,
              object : AppOpenAdManager.OnShowAdCompleteListener {
                override fun onShowAdComplete() {
                  Log.d(LOG_TAG, "[onActivityStarted] onShowAdComplete callback")
                }
              },
            )
          }
        }

        override fun onActivityResumed(activity: Activity) {
          Log.d(LOG_TAG, "[onActivityResumed] Activity: ${activity.javaClass.simpleName}")
        }

        override fun onActivityPaused(activity: Activity) {
          Log.d(LOG_TAG, "[onActivityPaused] Activity: ${activity.javaClass.simpleName}")
        }

        override fun onActivityStopped(activity: Activity) {
          Log.d(LOG_TAG, "[onActivityStopped] Activity: ${activity.javaClass.simpleName}")
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
          Log.d(LOG_TAG, "[onActivitySaveInstanceState] Activity: ${activity.javaClass.simpleName}")
        }

        override fun onActivityDestroyed(activity: Activity) {
          Log.d(LOG_TAG, "[onActivityDestroyed] Activity: ${activity.javaClass.simpleName}")
          if (currentActivity == activity) {
            currentActivity = null
          }
        }
      },
    )
  }

  /**
   * Loads an app open ad.
   */
  fun loadAd(activity: Activity) {
    Log.i(LOG_TAG, "[MyApplication.loadAd] Loading ad for activity: ${activity.javaClass.simpleName}")
    appOpenAdManager.loadAd(activity)
  }

  /**
   * Shows an app open ad if available.
   */
  fun showAdIfAvailable(
    activity: Activity,
    onShowAdCompleteListener: AppOpenAdManager.OnShowAdCompleteListener,
  ) {
    Log.i(LOG_TAG, "[MyApplication.showAdIfAvailable] Showing ad for activity: ${activity.javaClass.simpleName}")
    appOpenAdManager.showAdIfAvailable(activity, onShowAdCompleteListener)
  }
}

