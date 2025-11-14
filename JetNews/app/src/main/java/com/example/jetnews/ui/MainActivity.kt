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

package com.example.jetnews.ui

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.example.jetnews.GoogleMobileAdsConsentManager
import com.example.jetnews.MyApplication
import com.example.jetnews.R
import com.google.android.gms.ads.MobileAds

class MainActivity : AppCompatActivity() {

    private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(applicationContext)

        val appContainer = (application as MyApplication).container
        setContent {
            val widthSizeClass = calculateWindowSizeClass(this@MainActivity).widthSizeClass
            JetnewsApp(appContainer, widthSizeClass)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_privacy_settings -> {
                showPrivacyOptionsForm()
                true
            }
            R.id.menu_ad_inspector -> {
                openAdInspector()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Show the privacy options form if required.
     */
    private fun showPrivacyOptionsForm() {
        if (googleMobileAdsConsentManager.isPrivacyOptionsRequired) {
            googleMobileAdsConsentManager.showPrivacyOptionsForm(this) {
                Log.d(LOG_TAG, "Privacy options form dismissed")
            }
        } else {
            Log.d(LOG_TAG, "Privacy options form is not required")
        }
    }

    /**
     * Open Ad Inspector for debugging ads.
     */
    private fun openAdInspector() {
        MobileAds.openAdInspector(this) { adInspectorError ->
            if (adInspectorError != null) {
                Log.e(LOG_TAG, "Ad Inspector error: ${adInspectorError.code} - ${adInspectorError.message}")
            } else {
                Log.d(LOG_TAG, "Ad Inspector closed successfully")
            }
        }
    }

    companion object {
        private const val LOG_TAG = "MainActivity"
    }
}
