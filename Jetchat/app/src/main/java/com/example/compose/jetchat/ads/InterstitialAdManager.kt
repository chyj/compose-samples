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

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * 管理插页式广告的加载和显示
 * 
 * 重要说明：
 * - 使用 InterstitialAd.load 加载广告，避免重复加载
 * - 妥善处理 InterstitialAdLoadCallback，输出可调试的错误信息
 * - 在 FullScreenContentCallback 的所有路径中清空广告引用
 * - 若广告未准备好则恢复正常流程
 */
class InterstitialAdManager private constructor() {
    
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var onAdDismissedCallback: (() -> Unit)? = null
    
    companion object {
        private const val TAG = "InterstitialAdManager"
        
        // TODO: 替换为您的实际插页式广告位 ID
        // 测试广告位 ID: ca-app-pub-3940256099942544/1033173712
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
     * 
     * @param activity 当前 Activity
     * @param consentManager 同意管理器，用于检查是否可以加载广告
     */
    fun loadAd(activity: Activity, consentManager: GoogleMobileAdsConsentManager) {
        // 检查是否可以加载广告
        if (!consentManager.canLoadAds()) {
            Log.w(TAG, "无法加载广告：用户未同意或同意流程未完成")
            return
        }
        
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
                    
                    // 设置全屏内容回调
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "广告已关闭")
                            // 执行广告关闭后的回调（如果有）
                            onAdDismissedCallback?.invoke()
                            onAdDismissedCallback = null
                            // 清空广告引用
                            interstitialAd = null
                            // 可以在这里重新加载下一个广告
                            loadAd(activity, consentManager)
                        }
                        
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            Log.e(TAG, "广告显示失败: code=${error.code}, message=${error.message}, domain=${error.domain}")
                            // 执行回调（即使显示失败，也应该继续正常流程）
                            onAdDismissedCallback?.invoke()
                            onAdDismissedCallback = null
                            // 清空广告引用
                            interstitialAd = null
                        }
                        
                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "广告已显示")
                            // 清空广告引用，防止重复显示
                            interstitialAd = null
                        }
                        
                        override fun onAdClicked() {
                            Log.d(TAG, "用户点击了广告")
                        }
                        
                        override fun onAdImpression() {
                            Log.d(TAG, "广告展示已记录")
                        }
                    }
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(
                        TAG,
                        "插页式广告加载失败: code=${error.code}, message=${error.message}, " +
                            "domain=${error.domain}, cause=${error.cause}"
                    )
                    isLoading = false
                    interstitialAd = null
                    
                    // 输出详细的错误信息用于调试
                    when (error.code) {
                        AdRequest.ERROR_CODE_INTERNAL_ERROR -> {
                            Log.e(TAG, "内部错误：广告请求过程中发生内部错误")
                        }
                        AdRequest.ERROR_CODE_INVALID_REQUEST -> {
                            Log.e(TAG, "无效请求：广告请求无效")
                        }
                        AdRequest.ERROR_CODE_NETWORK_ERROR -> {
                            Log.e(TAG, "网络错误：由于网络连接问题，广告请求失败")
                        }
                        AdRequest.ERROR_CODE_NO_FILL -> {
                            Log.e(TAG, "无填充：广告请求成功，但没有广告可返回")
                        }
                        else -> {
                            Log.e(TAG, "未知错误代码: ${error.code}")
                        }
                    }
                }
            }
        )
    }
    
    /**
     * 显示插页式广告
     * 
     * @param activity 当前 Activity
     * @param onAdDismissed 广告关闭后的回调（可选）
     * @return true 如果广告已显示，false 如果广告未准备好
     */
    fun showAd(activity: Activity, onAdDismissed: (() -> Unit)? = null): Boolean {
        val ad = interstitialAd
        return if (ad != null) {
            // 保存回调，在广告关闭时执行
            onAdDismissedCallback = onAdDismissed
            ad.show(activity)
            true
        } else {
            Log.d(TAG, "广告未准备好，恢复正常流程")
            // 如果广告未准备好，立即执行回调（如果有）
            onAdDismissed?.invoke()
            false
        }
    }
    
    /**
     * 检查广告是否已准备好
     */
    fun isAdReady(): Boolean {
        return interstitialAd != null
    }
    
    /**
     * 清空当前广告引用（用于测试或重置）
     */
    fun clearAd() {
        interstitialAd = null
        isLoading = false
    }
}

