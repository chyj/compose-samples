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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.example.jetlagged.R
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

private const val TAG = "NativeAd"

/**
 * 原生广告 Composable
 *
 * @param adUnitId 广告单元 ID（测试 ID: "ca-app-pub-3940256099942544/2247696110"）
 * @param modifier Modifier
 * @param onAdLoaded 广告加载成功回调（可选）
 * @param onAdFailedToLoad 广告加载失败回调（可选）
 */
@Composable
fun NativeAd(
    adUnitId: String,
    modifier: Modifier = Modifier,
    onAdLoaded: ((NativeAd) -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 记住 NativeAdManager 实例
    val adManager = remember {
        NativeAdManager(context)
    }

    // 记住广告视图和当前广告
    var nativeAdView: NativeAdView? by remember { mutableStateOf(null) }
    var currentNativeAd: NativeAd? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }

    // 初始化 SDK
    LaunchedEffect(Unit) {
        adManager.initialize {
            isInitialized = true
        }
    }

    // 加载广告（当视图创建且 SDK 初始化完成后）
    LaunchedEffect(nativeAdView, isInitialized, adUnitId) {
        if (nativeAdView != null && isInitialized && !isLoading && currentNativeAd == null) {
            isLoading = true
            adManager.loadNativeAd(
                adUnitId = adUnitId,
                onAdLoaded = { nativeAd ->
                    Log.d(TAG, "Native ad loaded in Composable")
                    currentNativeAd = nativeAd
                    nativeAdView?.let { view ->
                        adManager.populateNativeAdView(view, nativeAd)
                    }
                    onAdLoaded?.invoke(nativeAd)
                    isLoading = false
                },
                onAdFailedToLoad = { error ->
                    Log.e(TAG, "Native ad failed to load in Composable: ${error.message}")
                    onAdFailedToLoad?.invoke(error)
                    isLoading = false
                },
            )
        }
    }

    // 生命周期管理：在销毁时清理广告
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_DESTROY) {
                Log.d(TAG, "Lifecycle ON_DESTROY, cleaning up native ad")
                currentNativeAd?.destroy()
                currentNativeAd = null
                nativeAdView = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            Log.d(TAG, "DisposableEffect onDispose, cleaning up native ad")
            currentNativeAd?.destroy()
            currentNativeAd = null
            nativeAdView = null
            adManager.destroy()
        }
    }

    // 使用 AndroidView 嵌入原生广告视图
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
    ) {
        AndroidView(
            factory = { ctx ->
                Log.d(TAG, "AndroidView factory called for native ad")
                val view = NativeAdView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                }
                // 加载布局
                val inflater = android.view.LayoutInflater.from(ctx)
                inflater.inflate(R.layout.ad_unified, view, true)
                nativeAdView = view

                // 如果广告已经加载，立即填充视图
                currentNativeAd?.let { ad ->
                    adManager.populateNativeAdView(view, ad)
                }

                FrameLayout(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                    addView(view)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            update = { frameLayout ->
                // 更新逻辑：如果广告已加载但视图未填充，则填充视图
                val view = frameLayout.getChildAt(0) as? NativeAdView
                if (view != null && currentNativeAd != null && !isLoading) {
                    adManager.populateNativeAdView(view, currentNativeAd!!)
                }
            },
        )
    }
}

