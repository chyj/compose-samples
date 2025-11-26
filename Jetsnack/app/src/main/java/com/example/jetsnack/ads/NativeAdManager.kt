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

package com.example.jetsnack.ads

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.example.jetsnack.databinding.AdUnifiedBinding
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 管理原生广告加载和显示的工具类
 */
class NativeAdManager(private val context: Context) {
    private var currentNativeAd: NativeAd? = null
    private val testAdUnitId = "ca-app-pub-3940256099942544/2247696110"

    /**
     * 初始化 AdMob SDK
     */
    suspend fun initializeAdMob() = withContext(Dispatchers.IO) {
        MobileAds.initialize(context) { initializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for (adapterClass in statusMap.keys) {
                val status = statusMap[adapterClass]
                Log.d(TAG, "Adapter: $adapterClass, Status: ${status?.initializationState}, " +
                    "Description: ${status?.description}")
            }
        }
    }

    /**
     * 加载原生广告
     */
    fun loadNativeAd(
        onAdLoaded: (NativeAd) -> Unit,
        onAdFailed: (String) -> Unit,
    ) {
        // 销毁旧广告
        currentNativeAd?.destroy()

        val adLoader = AdLoader.Builder(context, testAdUnitId)
            .forNativeAd { nativeAd ->
                Log.d(TAG, "原生广告加载成功")
                currentNativeAd = nativeAd
                onAdLoaded(nativeAd)
            }
            .withNativeAdOptions(
                com.google.android.gms.ads.nativead.NativeAdOptions.Builder()
                    .setVideoOptions(
                        com.google.android.gms.ads.VideoOptions.Builder()
                            .setStartMuted(true)
                            .build()
                    )
                    .build()
            )
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    val error = "加载失败: ${loadAdError.code} - ${loadAdError.message}"
                    Log.e(TAG, error)
                    onAdFailed(error)
                }

                override fun onAdClicked() {
                    Log.d(TAG, "广告被点击")
                }

                override fun onAdImpression() {
                    Log.d(TAG, "广告展示")
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    /**
     * 填充原生广告视图
     */
    fun populateNativeAdView(
        nativeAd: NativeAd,
        binding: AdUnifiedBinding,
    ) {
        val nativeAdView = binding.adUnified
        val mediaView = binding.adMedia
        val headlineView = binding.adHeadline
        val bodyView = binding.adBody
        val callToActionView = binding.adCallToAction
        val iconView = binding.adAppIcon
        val advertiserView = binding.adAdvertiser
        val starRatingView = binding.adStars
        val priceView = binding.adPrice
        val storeView = binding.adStore

        // 设置媒体视图
        nativeAdView.mediaView = mediaView

        // 设置标题
        headlineView.text = nativeAd.headline
        nativeAdView.headlineView = headlineView

        // 设置正文
        bodyView.text = nativeAd.body
        nativeAdView.bodyView = bodyView

        // 设置行动号召按钮
        callToActionView.text = nativeAd.callToAction
        nativeAdView.callToActionView = callToActionView

        // 设置应用图标（可选）
        if (nativeAd.icon == null) {
            iconView.visibility = View.GONE
            nativeAdView.iconView = null
        } else {
            iconView.setImageDrawable(nativeAd.icon?.drawable)
            iconView.visibility = View.VISIBLE
            nativeAdView.iconView = iconView
        }

        // 设置广告主名称（可选）
        if (nativeAd.advertiser == null) {
            advertiserView.visibility = View.GONE
            nativeAdView.advertiserView = null
        } else {
            advertiserView.text = nativeAd.advertiser
            advertiserView.visibility = View.VISIBLE
            nativeAdView.advertiserView = advertiserView
        }

        // 设置评分（可选）
        if (nativeAd.starRating == null) {
            starRatingView.visibility = View.GONE
            nativeAdView.starRatingView = null
        } else {
            starRatingView.rating = nativeAd.starRating!!.toFloat()
            starRatingView.visibility = View.VISIBLE
            nativeAdView.starRatingView = starRatingView
        }

        // 设置价格（可选）
        if (nativeAd.price == null) {
            priceView.visibility = View.GONE
            nativeAdView.priceView = null
        } else {
            priceView.text = nativeAd.price
            priceView.visibility = View.VISIBLE
            nativeAdView.priceView = priceView
        }

        // 设置商店名称（可选）
        if (nativeAd.store == null) {
            storeView.visibility = View.GONE
            nativeAdView.storeView = null
        } else {
            storeView.text = nativeAd.store
            storeView.visibility = View.VISIBLE
            nativeAdView.storeView = storeView
        }

        // 处理视频广告
        val videoController = nativeAd.mediaContent?.videoController
        if (videoController != null && videoController.hasVideoContent()) {
            videoController.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
                override fun onVideoStart() {
                    Log.d(TAG, "视频开始播放")
                }

                override fun onVideoPlay() {
                    Log.d(TAG, "视频播放")
                }

                override fun onVideoEnd() {
                    Log.d(TAG, "视频结束")
                }
            }
        }

        // 绑定广告到视图
        nativeAdView.setNativeAd(nativeAd)
    }

    /**
     * 销毁当前广告
     */
    fun destroy() {
        currentNativeAd?.destroy()
        currentNativeAd = null
    }

    companion object {
        private const val TAG = "NativeAdManager"
    }
}

