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

package com.example.jetlagged

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * 插页式广告管理器
 * 负责加载和显示插页式广告
 */
class InterstitialAdManager private constructor() {

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    companion object {
        private const val TAG = "InterstitialAdManager"
        
        // TODO: 替换为您的插页式广告单元 ID
        // 示例广告单元 ID（仅用于测试）：ca-app-pub-3940256099942544/1033173712
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        
        @Volatile
        private var INSTANCE: InterstitialAdManager? = null

        fun getInstance(): InterstitialAdManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: InterstitialAdManager().also { INSTANCE = it }
            }
        }
    }

    /**
     * 加载插页式广告
     * @param activity 当前 Activity
     */
    fun loadAd(activity: Activity) {
        // 避免重复加载
        if (isLoading || interstitialAd != null) {
            Log.d(TAG, "广告正在加载或已加载，跳过重复加载")
            return
        }

        isLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            activity,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "插页式广告加载成功")
                    interstitialAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(
                        TAG,
                        "插页式广告加载失败: ${loadAdError.message} (错误代码: ${loadAdError.code})"
                    )
                    interstitialAd = null
                    isLoading = false
                }
            }
        )
    }

    /**
     * 显示插页式广告
     * @param activity 当前 Activity
     * @return true 如果广告已显示，false 如果广告未准备好
     */
    fun showAd(activity: Activity): Boolean {
        interstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "广告已关闭")
                    // 清空广告引用
                    interstitialAd = null
                    // 加载下一个广告
                    loadAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(
                        TAG,
                        "广告展示失败: ${adError.message} (错误代码: ${adError.code})"
                    )
                    // 清空广告引用
                    interstitialAd = null
                    // 尝试加载下一个广告
                    loadAd(activity)
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "广告已展示")
                    // 广告展示时清空引用，防止重复显示
                    interstitialAd = null
                }

                override fun onAdClicked() {
                    Log.d(TAG, "用户点击了广告")
                }

                override fun onAdImpression() {
                    Log.d(TAG, "广告展示给用户")
                }
            }
            ad.show(activity)
            return true
        } ?: run {
            Log.d(TAG, "广告未准备好，无法显示")
            // 尝试加载广告
            loadAd(activity)
            return false
        }
    }

    /**
     * 检查广告是否已准备好
     */
    fun isAdReady(): Boolean {
        return interstitialAd != null
    }
}

