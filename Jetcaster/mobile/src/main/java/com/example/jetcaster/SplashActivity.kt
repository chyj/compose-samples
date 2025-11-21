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

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.jetcaster.ui.theme.JetcasterTheme
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.initialization.InitializationStatus

class SplashActivity : ComponentActivity() {
    companion object {
        private const val LOG_TAG = "OpenAdLifecycle"
        private const val SPLASH_DELAY_MS = 3000L
        private const val MAX_WAIT_TIME_MS = 5000L // 最多等待5秒广告加载
    }

    private var adShown = false
    private var adLoadCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set splash screen UI
        setContent {
            JetcasterTheme {
                SplashScreen()
            }
        }

        // Initialize Mobile Ads SDK
        initializeMobileAdsSdk()

        // Set up timeout: if ad doesn't load in time, proceed to MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            if (!adShown && !adLoadCompleted) {
                Log.d(LOG_TAG, "SplashActivity: Timeout reached, proceeding to MainActivity")
                proceedToMainActivity()
            }
        }, MAX_WAIT_TIME_MS)

        // Minimum splash delay
        Handler(Looper.getMainLooper()).postDelayed({
            checkAndShowAd()
        }, SPLASH_DELAY_MS)
    }

    private fun checkAndShowAd() {
        val application = application as JetcasterApplication
        Log.d(LOG_TAG, "SplashActivity: Checking if ad is available")
        
        if (application.appOpenAdManager.isAdAvailable()) {
            Log.d(LOG_TAG, "SplashActivity: Ad is available, showing it")
            adShown = true
            application.showAdIfAvailable(this) {
                // Ad dismissed callback
                Log.d(LOG_TAG, "SplashActivity: Ad dismissed, proceeding to MainActivity")
                proceedToMainActivity()
            }
            // Don't call proceedToMainActivity here, wait for ad dismiss callback
        } else {
            Log.d(LOG_TAG, "SplashActivity: Ad not available yet, waiting...")
            // Wait a bit more for ad to load
            Handler(Looper.getMainLooper()).postDelayed({
                if (!adShown) {
                    if (application.appOpenAdManager.isAdAvailable()) {
                        Log.d(LOG_TAG, "SplashActivity: Ad loaded, showing it")
                        adShown = true
                        application.showAdIfAvailable(this) {
                            // Ad dismissed callback
                            Log.d(LOG_TAG, "SplashActivity: Ad dismissed, proceeding to MainActivity")
                            proceedToMainActivity()
                        }
                    } else {
                        Log.d(LOG_TAG, "SplashActivity: Ad still not available, proceeding to MainActivity")
                        proceedToMainActivity()
                    }
                }
            }, 2000L)
        }
    }

    private fun proceedToMainActivity() {
        if (!isFinishing) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun initializeMobileAdsSdk() {
        Log.d(LOG_TAG, "SplashActivity: initializeMobileAdsSdk called")

        // Set test device IDs
        val testDeviceIds = listOf("TEST-DEVICE-HASHED-ID")
        val configuration = RequestConfiguration.Builder()
            .setTestDeviceIds(testDeviceIds)
            .build()
        MobileAds.setRequestConfiguration(configuration)

        // Gather consent first
        val consentManager = GoogleMobileAdsConsentManager.getInstance()
        Log.d(LOG_TAG, "SplashActivity: Starting UMP consent gathering")
        consentManager.gatherConsent(
            activity = this,
            skipConsentDialog = true,
            onConsentGathered = { canRequestAds ->
            if (canRequestAds) {
                Log.d(LOG_TAG, "SplashActivity: UMP consent completed, can request ads - initializing and loading")
                // Initialize MobileAds SDK after consent is granted
                MobileAds.initialize(this) { initializationStatus: InitializationStatus ->
                    Log.d(LOG_TAG, "SplashActivity: MobileAds.initialize completed after consent")
                    val statusMap = initializationStatus.adapterStatusMap
                    for (adapterClass in statusMap.keys) {
                        val status = statusMap[adapterClass]
                        Log.d(LOG_TAG, "SplashActivity: Adapter: $adapterClass, Status: ${status?.initializationState}, Latency: ${status?.latency}")
                    }

                    // Load ad on main thread after initialization
                    Handler(Looper.getMainLooper()).post {
                        val application = application as JetcasterApplication
                        Log.d(LOG_TAG, "SplashActivity: Calling loadAd after initialization")
                        application.appOpenAdManager.loadAd { adLoaded ->
                            adLoadCompleted = true
                            if (adLoaded && !adShown) {
                                Log.d(LOG_TAG, "SplashActivity: Ad loaded successfully, checking if we can show")
                                Handler(Looper.getMainLooper()).post {
                                    checkAndShowAd()
                                }
                            }
                        }
                    }
                }
            } else {
                Log.d(LOG_TAG, "SplashActivity: UMP consent completed, cannot request ads")
                adLoadCompleted = true
                if (!adShown) {
                    proceedToMainActivity()
                }
            }
            }
        )
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Jetcaster",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

