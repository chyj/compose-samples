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

package com.example.jetcaster.ads

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.example.jetcaster.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "NativeAdView"

/**
 * 原生广告单元 ID
 * 开发阶段使用测试 ID，发布前替换为实际 ID
 */
private const val NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"

/**
 * 在 Compose 中显示原生广告的可组合函数
 *
 * @param modifier 修饰符
 * @param adUnitId 广告单元 ID，默认为测试 ID
 * @param onAdLoaded 广告加载成功回调
 * @param onAdFailedToLoad 广告加载失败回调
 */
@Composable
fun NativeAdComposable(
    modifier: Modifier = Modifier,
    adUnitId: String = NATIVE_AD_UNIT_ID,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var isAdLoaded by remember { mutableStateOf(false) }

    // 初始化 AdMob SDK（如果尚未初始化）
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            MobileAds.initialize(context) { initializationStatus ->
                Log.d(TAG, "AdMob SDK initialized: ${initializationStatus.adapterStatusMap}")
            }
        }
    }

    // 加载原生广告
    LaunchedEffect(adUnitId) {
        if (isAdLoaded) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            loadNativeAd(
                context = context,
                adUnitId = adUnitId,
                onAdLoaded = { ad ->
                    nativeAd = ad
                    isAdLoaded = true
                    onAdLoaded?.invoke()
                },
                onAdFailedToLoad = { error ->
                    Log.e(TAG, "Failed to load native ad: $error")
                    onAdFailedToLoad?.invoke(error)
                }
            )
        }
    }

    // 使用 AndroidView 嵌入原生广告布局
    val adView = remember {
        LayoutInflater.from(context).inflate(
            R.layout.ad_unified,
            null
        ) as NativeAdView
    }

    AndroidView(
        factory = { adView },
        modifier = modifier.fillMaxWidth(),
        update = { view ->
            // 当广告更新时重新填充视图
            nativeAd?.let { ad ->
                populateNativeAdView(view, ad)
            }
        }
    )

    // 清理资源
    DisposableEffect(Unit) {
        onDispose {
            nativeAd?.destroy()
            nativeAd = null
        }
    }
}

/**
 * 加载原生广告
 */
private fun loadNativeAd(
    context: Context,
    adUnitId: String,
    onAdLoaded: (NativeAd) -> Unit,
    onAdFailedToLoad: (String) -> Unit,
) {
    val adLoader = AdLoader.Builder(context, adUnitId)
        .forNativeAd { nativeAd ->
            // 广告加载成功
            onAdLoaded(nativeAd)
        }
        .withNativeAdOptions(
            NativeAdOptions.Builder()
                .setRequestMultipleImages(false)
                .setReturnUrlsForImageAssets(false)
                .build()
        )
        .withAdListener(object : com.google.android.gms.ads.AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                val error = "Error code: ${loadAdError.code}, " +
                    "Error message: ${loadAdError.message}, " +
                    "Error domain: ${loadAdError.domain}"
                onAdFailedToLoad(error)
            }
        })
        .build()

    adLoader.loadAd(AdRequest.Builder().build())
}

/**
 * 填充原生广告视图
 */
private fun populateNativeAdView(adView: NativeAdView, nativeAd: NativeAd) {
    // 设置媒体视图
    val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
    adView.mediaView = mediaView

    // 设置标题
    val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
    headlineView.text = nativeAd.headline
    adView.headlineView = headlineView

    // 设置正文
    val bodyView = adView.findViewById<TextView>(R.id.ad_body)
    bodyView.text = nativeAd.body
    adView.bodyView = bodyView

    // 设置行动号召按钮
    val callToActionView = adView.findViewById<Button>(R.id.ad_call_to_action)
    callToActionView.text = nativeAd.callToAction
    adView.callToActionView = callToActionView

    // 设置应用图标（可选）
    val appIconView = adView.findViewById<ImageView>(R.id.ad_app_icon)
    if (nativeAd.icon != null) {
        appIconView.setImageDrawable(nativeAd.icon?.drawable)
        appIconView.visibility = View.VISIBLE
        adView.iconView = appIconView
    } else {
        appIconView.visibility = View.GONE
    }

    // 设置广告主名称（可选）
    val advertiserView = adView.findViewById<TextView>(R.id.ad_advertiser)
    if (nativeAd.advertiser != null) {
        advertiserView.text = nativeAd.advertiser
        advertiserView.visibility = View.VISIBLE
        adView.setAdvertiserView(advertiserView)
    } else {
        advertiserView.visibility = View.GONE
    }

    // 设置评分（可选）
    val starRatingView = adView.findViewById<RatingBar>(R.id.ad_stars)
    if (nativeAd.starRating != null) {
        starRatingView.rating = nativeAd.starRating!!.toFloat()
        starRatingView.visibility = View.VISIBLE
        adView.setStarRatingView(starRatingView)
    } else {
        starRatingView.visibility = View.GONE
    }

    // 设置价格（可选）
    val priceView = adView.findViewById<TextView>(R.id.ad_price)
    if (nativeAd.price != null) {
        priceView.text = nativeAd.price
        priceView.visibility = View.VISIBLE
        adView.setPriceView(priceView)
    } else {
        priceView.visibility = View.GONE
    }

    // 设置商店名称（可选）
    val storeView = adView.findViewById<TextView>(R.id.ad_store)
    if (nativeAd.store != null) {
        storeView.text = nativeAd.store
        storeView.visibility = View.VISIBLE
        adView.setStoreView(storeView)
    } else {
        storeView.visibility = View.GONE
    }

    // 显示价格和商店的容器
    val priceStoreContainer = priceView.parent as? View
    if (nativeAd.price != null || nativeAd.store != null) {
        priceStoreContainer?.visibility = View.VISIBLE
    } else {
        priceStoreContainer?.visibility = View.GONE
    }

    // 绑定原生广告到视图
    adView.setNativeAd(nativeAd)
}

