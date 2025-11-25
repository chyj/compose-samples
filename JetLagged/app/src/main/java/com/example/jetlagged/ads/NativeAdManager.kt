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

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.jetlagged.R
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MediaContent
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "NativeAdManager"

/**
 * 原生广告管理器
 * 负责加载和管理原生广告，处理广告生命周期
 */
class NativeAdManager(private val context: Context) {

    private var currentNativeAd: NativeAd? = null
    private var adLoader: AdLoader? = null
    private var isInitialized = false

    /**
     * 初始化 AdMob SDK
     */
    fun initialize(onInitialized: () -> Unit = {}) {
        if (isInitialized) {
            onInitialized()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                MobileAds.initialize(context) { initializationStatus ->
                    Log.d(TAG, "AdMob SDK initialized")
                    isInitialized = true
                    onInitialized()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize AdMob SDK", e)
            }
        }
    }

    /**
     * 加载原生广告
     * @param adUnitId 广告单元 ID（测试 ID: "ca-app-pub-3940256099942544/2247696110"）
     * @param onAdLoaded 广告加载成功回调
     * @param onAdFailedToLoad 广告加载失败回调
     */
    fun loadNativeAd(
        adUnitId: String,
        onAdLoaded: (NativeAd) -> Unit,
        onAdFailedToLoad: (LoadAdError) -> Unit = { error ->
            Log.e(TAG, "Failed to load native ad: ${error.message}, code: ${error.code}")
        },
    ) {
        // 销毁之前的广告
        currentNativeAd?.destroy()
        currentNativeAd = null

        if (!isInitialized) {
            Log.w(TAG, "AdMob SDK not initialized, initializing now...")
            initialize {
                loadNativeAd(adUnitId, onAdLoaded, onAdFailedToLoad)
            }
            return
        }

        Log.d(TAG, "Loading native ad with unit ID: $adUnitId")

        adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd: NativeAd ->
                Log.d(TAG, "Native ad loaded successfully")
                currentNativeAd = nativeAd
                onAdLoaded(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(
                        TAG,
                        "Native ad failed to load: ${loadAdError.message}, " +
                            "code: ${loadAdError.code}, " +
                            "domain: ${loadAdError.domain}",
                    )
                    onAdFailedToLoad(loadAdError)
                }

                override fun onAdClicked() {
                    Log.d(TAG, "Native ad clicked")
                }

                override fun onAdImpression() {
                    Log.d(TAG, "Native ad impression recorded")
                }

                override fun onAdOpened() {
                    Log.d(TAG, "Native ad opened")
                }

                override fun onAdClosed() {
                    Log.d(TAG, "Native ad closed")
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setVideoOptions(
                        com.google.android.gms.ads.VideoOptions.Builder()
                            .setStartMuted(true)
                            .build(),
                    )
                    .build(),
            )
            .build()

        val adRequest = AdRequest.Builder().build()
        adLoader?.loadAd(adRequest)
    }

    /**
     * 填充原生广告视图
     * @param nativeAdView 原生广告视图容器
     * @param nativeAd 原生广告对象
     */
    fun populateNativeAdView(nativeAdView: NativeAdView, nativeAd: NativeAd) {
        // 设置 MediaView
        val mediaView = nativeAdView.findViewById<MediaView>(R.id.ad_media)
        nativeAdView.mediaView = mediaView

        // 设置标题
        val headlineView = nativeAdView.findViewById<TextView>(R.id.ad_headline)
        headlineView.text = nativeAd.headline
        nativeAdView.setHeadlineView(headlineView)

        // 设置正文
        val bodyView = nativeAdView.findViewById<TextView>(R.id.ad_body)
        if (nativeAd.body == null) {
            bodyView.visibility = View.GONE
        } else {
            bodyView.visibility = View.VISIBLE
            bodyView.text = nativeAd.body
        }
        nativeAdView.setBodyView(bodyView)

        // 设置行动号召按钮
        val callToActionView = nativeAdView.findViewById<Button>(R.id.ad_call_to_action)
        if (nativeAd.callToAction == null) {
            callToActionView.visibility = View.GONE
        } else {
            callToActionView.visibility = View.VISIBLE
            callToActionView.text = nativeAd.callToAction
        }
        nativeAdView.setCallToActionView(callToActionView)

        // 设置应用图标（可选）
        val iconView = nativeAdView.findViewById<ImageView>(R.id.ad_app_icon)
        if (nativeAd.icon == null) {
            iconView.visibility = View.GONE
        } else {
            iconView.visibility = View.VISIBLE
            iconView.setImageDrawable(nativeAd.icon?.drawable)
            nativeAdView.setIconView(iconView)
        }

        // 设置广告主名称（可选）
        val advertiserView = nativeAdView.findViewById<TextView>(R.id.ad_advertiser)
        if (nativeAd.advertiser == null) {
            advertiserView.visibility = View.GONE
        } else {
            advertiserView.visibility = View.VISIBLE
            advertiserView.text = nativeAd.advertiser
        }
        nativeAdView.setAdvertiserView(advertiserView)

        // 设置评分（可选）
        val starRatingView = nativeAdView.findViewById<RatingBar>(R.id.ad_stars)
        if (nativeAd.starRating == null) {
            starRatingView.visibility = View.GONE
        } else {
            starRatingView.visibility = View.VISIBLE
            starRatingView.rating = nativeAd.starRating!!.toFloat()
            nativeAdView.setStarRatingView(starRatingView)
        }

        // 设置价格（可选）
        val priceView = nativeAdView.findViewById<TextView>(R.id.ad_price)
        if (nativeAd.price == null) {
            priceView.visibility = View.GONE
        } else {
            priceView.visibility = View.VISIBLE
            priceView.text = nativeAd.price
        }
        nativeAdView.setPriceView(priceView)

        // 设置商店名称（可选）
        val storeView = nativeAdView.findViewById<TextView>(R.id.ad_store)
        if (nativeAd.store == null) {
            storeView.visibility = View.GONE
        } else {
            storeView.visibility = View.VISIBLE
            storeView.text = nativeAd.store
        }
        nativeAdView.setStoreView(storeView)

        // 显示价格和商店容器（如果至少有一个存在）
        val priceStoreContainer = nativeAdView.findViewById<View>(R.id.ad_price_store_container)
        if (nativeAd.price == null && nativeAd.store == null) {
            priceStoreContainer.visibility = View.GONE
        } else {
            priceStoreContainer.visibility = View.VISIBLE
        }

        // 处理视频广告
        val mediaContent = nativeAd.mediaContent
        if (mediaContent != null && mediaContent.hasVideoContent()) {
            val videoController = mediaContent.videoController
            videoController.setVideoLifecycleCallbacks(object : VideoController.VideoLifecycleCallbacks() {
                override fun onVideoStart() {
                    Log.d(TAG, "Video ad started")
                }

                override fun onVideoPlay() {
                    Log.d(TAG, "Video ad playing")
                }

                override fun onVideoPause() {
                    Log.d(TAG, "Video ad paused")
                }

                override fun onVideoEnd() {
                    Log.d(TAG, "Video ad ended")
                }

                override fun onVideoMute(isMuted: Boolean) {
                    Log.d(TAG, "Video ad muted: $isMuted")
                }
            })
        }

        // 将原生广告绑定到视图
        nativeAdView.setNativeAd(nativeAd)
    }

    /**
     * 销毁当前广告
     */
    fun destroy() {
        currentNativeAd?.destroy()
        currentNativeAd = null
        adLoader = null
        Log.d(TAG, "Native ad destroyed")
    }

    /**
     * 获取当前广告（用于生命周期管理）
     */
    fun getCurrentNativeAd(): NativeAd? = currentNativeAd
}

