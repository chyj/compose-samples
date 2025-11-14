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
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import com.example.jetnews.data.AppContainer
import com.example.jetnews.data.AppContainerImpl
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Application class that initializes the app and handles App Open Ad lifecycle.
 */
class MyApplication : MultiDexApplication() {
    companion object {
        const val JETNEWS_APP_URI = "https://developer.android.com/jetnews"
        
        // Test device hashed ID - will be logged in onCreate
        const val TEST_DEVICE_HASHED_ID = "33BE2250B43518CCDA7DE426D04EE231"
        
        private const val LOG_TAG = "MyApplication"
    }

    // AppContainer instance used by the rest of classes to obtain dependencies
    lateinit var container: AppContainer
    
    private lateinit var appOpenAdManager: AppOpenAdManager
    var currentActivity: Activity? = null

    /**
     * Interface definition for a callback to be invoked when an app open ad is complete
     * (i.e. dismissed or fails to show).
     */
    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(LOG_TAG, "MyApplication.onCreate() - Starting initialization")
        
        try {
            container = AppContainerImpl(this)
            Log.d(LOG_TAG, "AppContainer initialized successfully")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error initializing AppContainer", e)
            throw e
        }
        
        try {
            // Initialize AppOpenAdManager
            appOpenAdManager = AppOpenAdManager(this)
            Log.d(LOG_TAG, "AppOpenAdManager initialized successfully")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error initializing AppOpenAdManager", e)
            throw e
        }
        
        try {
            // Register ActivityLifecycleCallbacks
            registerActivityLifecycleCallbacks(appOpenAdManager)
            Log.d(LOG_TAG, "ActivityLifecycleCallbacks registered successfully")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error registering ActivityLifecycleCallbacks", e)
        }
        
        try {
            // Register ProcessLifecycleOwner observer
            ProcessLifecycleOwner.get().lifecycle.addObserver(appOpenAdManager)
            Log.d(LOG_TAG, "ProcessLifecycleOwner observer registered successfully")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error registering ProcessLifecycleOwner observer", e)
        }
        
        // Log test device ID for debugging
        logTestDeviceId()
        
        Log.d(LOG_TAG, "MyApplication.onCreate() - Initialization completed")
    }

    /**
     * Load an app open ad.
     */
    fun loadAd(context: android.content.Context) {
        Log.d(LOG_TAG, "loadAd() called with context: ${context.javaClass.simpleName}")
        try {
            appOpenAdManager.loadAd(context)
            Log.d(LOG_TAG, "loadAd() completed successfully")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error in loadAd()", e)
        }
    }

    /**
     * Show the app open ad if one is available.
     */
    fun showAdIfAvailable(
        activity: Activity,
        onShowAdCompleteListener: OnShowAdCompleteListener
    ) {
        Log.d(LOG_TAG, "showAdIfAvailable() called with activity: ${activity.javaClass.simpleName}")
        currentActivity = activity
        try {
            appOpenAdManager.showAdIfAvailable(activity, onShowAdCompleteListener)
            Log.d(LOG_TAG, "showAdIfAvailable() completed successfully")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error in showAdIfAvailable()", e)
            // Call the listener even if there's an error to prevent blocking
            onShowAdCompleteListener.onShowAdComplete()
        }
    }

    /**
     * Log the test device ID for debugging purposes.
     */
    private fun logTestDeviceId() {
        try {
            val info = packageManager.getPackageInfo(packageName, 0)
            val testDeviceId = getTestDeviceId()
            Log.d(LOG_TAG, "Test Device ID: $testDeviceId")
            Log.d(LOG_TAG, "Add this to your test device list in AdMob console")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error getting test device ID", e)
        }
    }

    /**
     * Get the test device ID hash.
     */
    private fun getTestDeviceId(): String {
        val androidId = android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
        return hashDeviceId(androidId)
    }

    /**
     * Hash device ID using MD5.
     */
    private fun hashDeviceId(deviceId: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val hashBytes = md.digest(deviceId.toByteArray())
            hashBytes.joinToString("") { "%02X".format(it) }
        } catch (e: NoSuchAlgorithmException) {
            Log.e(LOG_TAG, "Error hashing device ID", e)
            ""
        }
    }
}
