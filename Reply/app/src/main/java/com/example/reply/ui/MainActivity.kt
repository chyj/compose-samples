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

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reply.data.local.LocalEmailsDataProvider
import com.example.reply.ui.theme.ContrastAwareReplyTheme
import com.google.accompanist.adaptive.calculateDisplayFeatures
import com.google.android.gms.ads.MobileAds

private const val TAG = "OpenAdLifecycle"

class MainActivity : ComponentActivity() {

    private val viewModel: ReplyHomeViewModel by viewModels()
    private var consentManager: GoogleMobileAdsConsentManager? = null

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity: onCreate")

        consentManager = GoogleMobileAdsConsentManager(this)

        setContent {
            ContrastAwareReplyTheme {
                val windowSize = calculateWindowSizeClass(this)
                val displayFeatures = calculateDisplayFeatures(this)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                ReplyApp(
                    windowSize = windowSize,
                    displayFeatures = displayFeatures,
                    replyHomeUIState = uiState,
                    closeDetailScreen = {
                        viewModel.closeDetailScreen()
                    },
                    navigateToDetail = { emailId, pane ->
                        viewModel.setOpenedEmail(emailId, pane)
                    },
                    toggleSelectedEmail = { emailId ->
                        viewModel.toggleSelectedEmail(emailId)
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MainActivity: onResume")
        (application as ReplyApplication).appOpenAdManager.setCurrentActivity(this)

        // 检查并显示隐私选项表单（如果需要）
        consentManager?.let { manager ->
            if (manager.canRequestAds()) {
                // 如果需要显示隐私选项表单，可以在这里调用
                // manager.showPrivacyOptionsForm()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "MainActivity: onPause")
        (application as ReplyApplication).appOpenAdManager.setCurrentActivity(null)
    }

    /**
     * 打开广告检查器（用于调试）
     */
    fun openAdInspector() {
        Log.d(TAG, "openAdInspector: Opening ad inspector")
        MobileAds.openAdInspector(this) { adInspectorError ->
            if (adInspectorError != null) {
                Log.e(TAG, "openAdInspector: Error: ${adInspectorError.message}")
            } else {
                Log.d(TAG, "openAdInspector: Ad inspector closed")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true)
@Composable
fun ReplyAppPreview() {
    ContrastAwareReplyTheme {
        ReplyApp(
            replyHomeUIState = ReplyHomeUIState(emails = LocalEmailsDataProvider.allEmails),
            windowSize = WindowSizeClass.calculateFromSize(DpSize(400.dp, 900.dp)),
            displayFeatures = emptyList(),
        )
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true, widthDp = 700, heightDp = 500)
@Composable
fun ReplyAppPreviewTablet() {
    ContrastAwareReplyTheme {
        ReplyApp(
            replyHomeUIState = ReplyHomeUIState(emails = LocalEmailsDataProvider.allEmails),
            windowSize = WindowSizeClass.calculateFromSize(DpSize(700.dp, 500.dp)),
            displayFeatures = emptyList(),
        )
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true, widthDp = 500, heightDp = 700)
@Composable
fun ReplyAppPreviewTabletPortrait() {
    ContrastAwareReplyTheme {
        ReplyApp(
            replyHomeUIState = ReplyHomeUIState(emails = LocalEmailsDataProvider.allEmails),
            windowSize = WindowSizeClass.calculateFromSize(DpSize(500.dp, 700.dp)),
            displayFeatures = emptyList(),
        )
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true, widthDp = 1100, heightDp = 600)
@Composable
fun ReplyAppPreviewDesktop() {
    ContrastAwareReplyTheme {
        ReplyApp(
            replyHomeUIState = ReplyHomeUIState(emails = LocalEmailsDataProvider.allEmails),
            windowSize = WindowSizeClass.calculateFromSize(DpSize(1100.dp, 600.dp)),
            displayFeatures = emptyList(),
        )
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true, widthDp = 600, heightDp = 1100)
@Composable
fun ReplyAppPreviewDesktopPortrait() {
    ContrastAwareReplyTheme {
        ReplyApp(
            replyHomeUIState = ReplyHomeUIState(emails = LocalEmailsDataProvider.allEmails),
            windowSize = WindowSizeClass.calculateFromSize(DpSize(600.dp, 1100.dp)),
            displayFeatures = emptyList(),
        )
    }
}
