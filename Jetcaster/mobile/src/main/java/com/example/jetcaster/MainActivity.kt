/*
 * Copyright 2020-2025 The Android Open Source Project
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

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.jetcaster.glancewidget.updateWidgetPreview
import com.example.jetcaster.ui.JetcasterApp
import com.example.jetcaster.ui.theme.JetcasterTheme
import com.google.accompanist.adaptive.calculateDisplayFeatures
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val LOG_TAG = "OpenAdLifecycle"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        updateWidgetPreview(this)
        setContent {
            val displayFeatures = calculateDisplayFeatures(this)

            JetcasterTheme {
                JetcasterApp(
                    displayFeatures,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Show privacy options if needed
        val consentManager = GoogleMobileAdsConsentManager.getInstance()
        if (consentManager.privacyOptionsRequirementStatus() ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        ) {
            // Show privacy options form
            consentManager.showPrivacyOptionsForm(
                this,
                object : ConsentForm.OnConsentFormDismissedListener {
                    override fun onConsentFormDismissed(error: com.google.android.ump.FormError?) {
                        if (error != null) {
                            Log.d(LOG_TAG, "MainActivity: Privacy options form dismissed with error - ${error.message}")
                        } else {
                            Log.d(LOG_TAG, "MainActivity: Privacy options form dismissed successfully")
                        }
                    }
                }
            )
        }
    }

    /**
     * Opens Ad Inspector for debugging ads.
     */
    fun openAdInspector() {
        MobileAds.openAdInspector(this) { adInspectorError ->
            if (adInspectorError != null) {
                Log.d(LOG_TAG, "MainActivity: Ad Inspector error - ${adInspectorError.message}")
            } else {
                Log.d(LOG_TAG, "MainActivity: Ad Inspector closed")
            }
        }
    }
}

