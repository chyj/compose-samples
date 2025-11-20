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

package com.example.compose.jetchat.ads

import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
 * BannerAd Composable 组件
 * 
 * 使用 AndroidView 托管 AdView，通过 remember/DisposableEffect 管理生命周期
 * 
 * @param adUnitId 广告位 ID（示例：ca-app-pub-3940256099942544/6300978111）
 * @param modifier Modifier 用于样式调整
 * @param canRequestAds 是否可以请求广告（从 GoogleMobileAdsConsentManager 获取）
 */
@Composable
fun BannerAd(
    adUnitId: String,
    modifier: Modifier = Modifier,
    canRequestAds: Boolean = true,
) {
    val context = LocalContext.current
    var adView by remember { mutableStateOf<AdView?>(null) }
    
    // 只有在可以请求广告时才创建和加载广告
    if (!canRequestAds) {
        Log.d(TAG, "BannerAd: cannot request ads, skipping ad load")
        return
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp) // Banner 标准高度
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        AndroidView(
            factory = { ctx ->
                Log.d(TAG, "BannerAd: creating AdView for adUnitId: $adUnitId")
                AdView(ctx).apply {
                    adView = this
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = adUnitId
                    
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            Log.d(TAG, "BannerAd: ad loaded successfully for adUnitId: $adUnitId")
                        }
                        
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            val errorCode = loadAdError.code
                            val errorDomain = loadAdError.domain
                            val errorMessage = loadAdError.message
                            Log.e(
                                TAG,
                                "BannerAd: ad failed to load. ErrorCode: $errorCode, " +
                                        "Domain: $errorDomain, Message: $errorMessage"
                            )
                        }
                        
                        override fun onAdOpened() {
                            Log.d(TAG, "BannerAd: ad opened")
                        }
                        
                        override fun onAdClosed() {
                            Log.d(TAG, "BannerAd: ad closed")
                        }
                        
                        override fun onAdClicked() {
                            Log.d(TAG, "BannerAd: ad clicked")
                        }
                        
                        override fun onAdImpression() {
                            Log.d(TAG, "BannerAd: ad impression recorded")
                        }
                    }
                    
                    // 设置布局参数
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    
                    // 加载广告请求
                    Log.d(TAG, "BannerAd: loading ad request for adUnitId: $adUnitId")
                    val adRequest = AdRequest.Builder().build()
                    loadAd(adRequest)
                }
            },
            update = { view ->
                // 如果 adUnitId 改变，更新 AdView 并重新加载广告
                if (view.adUnitId != adUnitId) {
                    Log.d(TAG, "BannerAd: updating adUnitId from ${view.adUnitId} to $adUnitId")
                    view.adUnitId = adUnitId
                    val adRequest = AdRequest.Builder().build()
                    view.loadAd(adRequest)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
    
    // 使用 DisposableEffect 管理广告生命周期，确保在 Composable 销毁时释放资源
    DisposableEffect(adUnitId, canRequestAds) {
        onDispose {
            Log.d(TAG, "BannerAd: disposing AdView for adUnitId: $adUnitId")
            adView?.destroy()
            adView = null
        }
    }
}

