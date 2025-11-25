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

package com.example.jetlagged.ads

import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
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
 * @param adUnitId 广告位 ID（测试 ID: "ca-app-pub-3940256099942544/6300978111"）
 * @param modifier Modifier
 */
@Composable
fun BannerAd(
    adUnitId: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    
    // 记住 AdView 实例
    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            this.adUnitId = adUnitId
            Log.d(TAG, "AdView created with adUnitId: $adUnitId")
        }
    }
    
    // 记住是否已加载广告，避免重复加载
    var adLoaded by remember { mutableStateOf(false) }
    
    // 设置 AdListener 用于日志记录
    DisposableEffect(adView) {
        val adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d(TAG, "Ad loaded successfully for unit: $adUnitId")
            }
            
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e(TAG, "Ad failed to load for unit: $adUnitId, " +
                        "errorCode: ${loadAdError.code}, " +
                        "message: ${loadAdError.message}, " +
                        "domain: ${loadAdError.domain}, " +
                        "cause: ${loadAdError.cause}")
            }
            
            override fun onAdOpened() {
                Log.d(TAG, "Ad opened for unit: $adUnitId")
            }
            
            override fun onAdClicked() {
                Log.d(TAG, "Ad clicked for unit: $adUnitId")
            }
            
            override fun onAdClosed() {
                Log.d(TAG, "Ad closed for unit: $adUnitId")
            }
            
            override fun onAdImpression() {
                Log.d(TAG, "Ad impression recorded for unit: $adUnitId")
            }
        }
        
        adView.adListener = adListener
        Log.d(TAG, "AdListener attached to AdView")
        
        onDispose {
            Log.d(TAG, "Disposing AdView for unit: $adUnitId")
            // 移除 AdListener（使用空实现替代 null）
            adView.adListener = object : AdListener() {}
            adView.destroy()
        }
    }
    
    // 加载广告
    LaunchedEffect(adLoaded) {
        if (!adLoaded) {
            Log.d(TAG, "Loading ad for unit: $adUnitId")
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
            adLoaded = true
        }
    }
    
    // 使用 AndroidView 嵌入 AdView
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
    ) {
        AndroidView(
            factory = { ctx ->
                Log.d(TAG, "AndroidView factory called for unit: $adUnitId")
                FrameLayout(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                    addView(adView)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            update = { frameLayout ->
                // 更新逻辑（如果需要）
                Log.d(TAG, "AndroidView update called for unit: $adUnitId")
            },
        )
    }
}

