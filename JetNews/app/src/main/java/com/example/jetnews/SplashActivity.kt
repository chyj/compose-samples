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

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.jetnews.R
import com.example.jetnews.ui.MainActivity
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Splash Activity that shows a countdown timer and loads/displays App Open Ad.
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private val gatherConsentFinished = AtomicBoolean(false)
    private var secondsRemaining: Long = 0L
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(LOG_TAG, "SplashActivity.onCreate() - Starting")
        
        try {
            setContentView(R.layout.activity_splash)
            Log.d(LOG_TAG, "SplashActivity layout inflated successfully")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error inflating layout", e)
            throw e
        }

        // Log the Mobile Ads SDK version.
        try {
            val sdkVersion = MobileAds.getVersion()
            Log.d(LOG_TAG, "Google Mobile Ads SDK Version: $sdkVersion")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error getting Mobile Ads SDK version", e)
        }

        // Create a timer so the SplashActivity will be displayed for a fixed amount of time.
        try {
            createTimer()
            Log.d(LOG_TAG, "Countdown timer created successfully")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error creating timer", e)
        }

        // 绕过用户同意，直接设置为已完成
        try {
            googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(applicationContext)
            Log.d(LOG_TAG, "GoogleMobileAdsConsentManager instance obtained")
            
            // 直接设置同意收集为已完成，绕过用户同意流程
            gatherConsentFinished.set(true)
            Log.d(LOG_TAG, "Bypassing consent gathering, directly initializing SDK")
            
            // 直接初始化 MobileAds SDK
            initializeMobileAdsSdk()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error initializing SDK", e)
            gatherConsentFinished.set(true)
        }
        
        Log.d(LOG_TAG, "SplashActivity.onCreate() - Completed")
    }

    /**
     * Create the timer, which waits for a fixed amount of time and then shows the app open ad.
     * The loading spinner is already displayed in the layout.
     */
    private fun createTimer() {
        // 加载圈已经在布局中显示，不需要更新文本
        countDownTimer = object : CountDownTimer(COUNTER_TIME_MILLISECONDS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1
                // 不再更新倒计时文本，加载圈会自动显示
            }

            override fun onFinish() {
                secondsRemaining = 0

                (application as MyApplication).showAdIfAvailable(
                    this@SplashActivity,
                    object : MyApplication.OnShowAdCompleteListener {
                        override fun onShowAdComplete() {
                            // 直接启动主界面，不再检查同意表单
                            startMainActivity()
                        }
                    },
                )
            }
        }
        countDownTimer?.start()
    }

    private fun initializeMobileAdsSdk() {
        Log.d(LOG_TAG, "initializeMobileAdsSdk() - Called")
        
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            Log.d(LOG_TAG, "initializeMobileAdsSdk() - Already initialized, skipping")
            return
        }

        try {
            // Set your test devices.
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(listOf(MyApplication.TEST_DEVICE_HASHED_ID))
                    .build()
            )
            Log.d(LOG_TAG, "RequestConfiguration set with test device: ${MyApplication.TEST_DEVICE_HASHED_ID}")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error setting RequestConfiguration", e)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(LOG_TAG, "Initializing MobileAds SDK on background thread")
                // Initialize the Google Mobile Ads SDK on a background thread.
                MobileAds.initialize(this@SplashActivity) { initializationStatus ->
                    Log.d(LOG_TAG, "MobileAds.initialize() callback - Status: ${initializationStatus.adapterStatusMap}")
                    Log.d(LOG_TAG, "MobileAds.initialize() callback - State: ${initializationStatus.adapterStatusMap.values}")
                }
                
                runOnUiThread {
                    try {
                        Log.d(LOG_TAG, "Loading ad on main thread")
                        // Load an ad on the main thread.
                        (application as? MyApplication)?.loadAd(this@SplashActivity)
                            ?: Log.e(LOG_TAG, "Application is not MyApplication instance")
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "Error loading ad on main thread", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Error initializing MobileAds SDK", e)
            }
        }
    }

    /** Start the MainActivity. */
    fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    companion object {
        // Number of milliseconds to count down before showing the app open ad. This simulates the time
        // needed to load the app.
        private const val COUNTER_TIME_MILLISECONDS = 10000L // 10 seconds

        private const val LOG_TAG = "SplashActivity"
    }
}
