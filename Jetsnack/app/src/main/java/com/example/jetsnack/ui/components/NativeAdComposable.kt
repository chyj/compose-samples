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

package com.example.jetsnack.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.jetsnack.ads.NativeAdManager
import com.example.jetsnack.databinding.AdUnifiedBinding
import com.google.android.gms.ads.nativead.NativeAd

/**
 * Compose 可组合函数，用于显示原生广告
 */
@Composable
fun NativeAdView(
    modifier: Modifier = Modifier,
    adUnitId: String? = null,
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    val adManager = remember { NativeAdManager(context) }

    // 初始化 AdMob SDK
    LaunchedEffect(Unit) {
        adManager.initializeAdMob()
    }

    // 加载广告
    LaunchedEffect(Unit) {
        adManager.loadNativeAd(
            onAdLoaded = { ad ->
                nativeAd = ad
            },
            onAdFailed = { error ->
                android.util.Log.e("NativeAdView", "广告加载失败: $error")
            }
        )
    }

    // 清理资源
    DisposableEffect(Unit) {
        onDispose {
            nativeAd?.destroy()
            adManager.destroy()
        }
    }

    // 显示广告视图
    AndroidView(
        factory = { ctx ->
            val binding = AdUnifiedBinding.inflate(
                android.view.LayoutInflater.from(ctx),
                null,
                false
            )
            // 如果已经有广告，立即填充
            nativeAd?.let { ad ->
                adManager.populateNativeAdView(ad, binding)
            }
            binding.root
        },
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        update = { view ->
            // 当广告加载完成时更新视图
            nativeAd?.let { ad ->
                val binding = AdUnifiedBinding.bind(view)
                adManager.populateNativeAdView(ad, binding)
            }
        }
    )
}

