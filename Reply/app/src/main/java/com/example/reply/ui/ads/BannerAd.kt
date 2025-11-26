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

package com.example.reply.ui.ads

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
 * @param adUnitId 广告位 ID（使用测试 ID: "ca-app-pub-3940256099942544/6300978111"）
 * @param modifier Modifier
 */
@Composable
fun BannerAd(
    adUnitId: String,
    modifier: Modifier = Modifier,
) {
    val consentManager = remember { GoogleMobileAdsConsentManager.getInstance() }
    
    // 检查是否可以请求广告
    if (!consentManager.canRequestAds) {
        Log.w(TAG, "无法请求广告：合规授权未完成")
        return
    }
    
    Log.d(TAG, "创建 BannerAd，adUnitId: $adUnitId")
    
    val adViewRef = remember { mutableStateOf<AdView?>(null) }
    
    AndroidView(
        factory = { ctx ->
            Log.d(TAG, "创建 AdView")
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d(TAG, "Banner 广告加载成功")
                    }
                    
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.e(
                            TAG,
                            "Banner 广告加载失败: code=${loadAdError.code}, " +
                                "message=${loadAdError.message}, " +
                                "domain=${loadAdError.domain}, " +
                                "cause=${loadAdError.cause}"
                        )
                    }
                    
                    override fun onAdOpened() {
                        Log.d(TAG, "Banner 广告被打开")
                    }
                    
                    override fun onAdClosed() {
                        Log.d(TAG, "Banner 广告被关闭")
                    }
                    
                    override fun onAdClicked() {
                        Log.d(TAG, "Banner 广告被点击")
                    }
                    
                    override fun onAdImpression() {
                        Log.d(TAG, "Banner 广告展示")
                    }
                }
                
                // 加载广告请求
                Log.d(TAG, "开始加载广告请求")
                val adRequest = AdRequest.Builder().build()
                loadAd(adRequest)
                
                adViewRef.value = this
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp) // Banner 标准高度为 50dp
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
    
    DisposableEffect(adUnitId) {
        onDispose {
            Log.d(TAG, "清理 AdView 资源")
            adViewRef.value?.destroy()
        }
    }
}

