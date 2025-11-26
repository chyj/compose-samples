/*
 * Copyright 2025 The Android Open Source Project
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

package com.example.jetcaster.ads

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val TAG = "BannerAd"

/**
 * Banner 广告 Composable
 *
 * @param adUnitId 广告位 ID（示例：ca-app-pub-3940256099942544/6300978111）
 * @param modifier Modifier
 */
@Composable
fun BannerAd(
    adUnitId: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val canLoadAd = remember { mutableStateOf(true) }
    val adView = remember { mutableStateOf<AdView?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 8.dp),
    ) {
        AndroidView(
            factory = { ctx ->
                Log.d(TAG, "BannerAd - 创建 AdView: $adUnitId")
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = adUnitId
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            Log.d(TAG, "BannerAd - 广告加载成功: $adUnitId")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.e(
                                TAG,
                                "BannerAd - 广告加载失败: $adUnitId, " +
                                    "错误码: ${error.code}, 错误信息: ${error.message}",
                            )
                        }

                        override fun onAdOpened() {
                            Log.d(TAG, "BannerAd - 广告打开: $adUnitId")
                        }

                        override fun onAdClosed() {
                            Log.d(TAG, "BannerAd - 广告关闭: $adUnitId")
                        }

                        override fun onAdClicked() {
                            Log.d(TAG, "BannerAd - 广告被点击: $adUnitId")
                        }

                        override fun onAdImpression() {
                            Log.d(TAG, "BannerAd - 广告展示: $adUnitId")
                        }
                    }
                    adView.value = this
                }
            },
            modifier = Modifier.fillMaxWidth(),
            update = { view ->
                // 当 canLoadAd 状态改变时，尝试加载广告
                if (canLoadAd.value && view.adUnitId == adUnitId) {
                    Log.d(TAG, "BannerAd - 更新时尝试加载广告: $adUnitId")
                    val adRequest = AdRequest.Builder().build()
                    view.loadAd(adRequest)
                }
            },
        )
    }

    // 加载广告
    LaunchedEffect(adUnitId) {
        if (canLoadAd.value && adView.value != null) {
            Log.d(TAG, "BannerAd - LaunchedEffect: 开始加载广告: $adUnitId")
            val adRequest = AdRequest.Builder().build()
            adView.value?.loadAd(adRequest)
        }
    }

    // 控制广告生命周期
    DisposableEffect(adUnitId) {
        Log.d(TAG, "BannerAd - DisposableEffect 创建: $adUnitId")
        
        onDispose {
            Log.d(TAG, "BannerAd - DisposableEffect 清理: $adUnitId")
            adView.value?.destroy()
            adView.value = null
        }
    }
}

