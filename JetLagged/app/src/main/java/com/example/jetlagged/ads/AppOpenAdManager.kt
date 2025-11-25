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

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

/**
 * App Open Ad 管理器
 * 负责加载、显示 App Open Ad，管理广告生命周期和状态
 */
class AppOpenAdManager(private val application: Application) : DefaultLifecycleObserver {
    companion object {
        private const val TAG = "AdMob_AppOpen"
        // App Open Ad 测试广告单元 ID
        // 注意：如果使用自己的广告单元 ID，请确保在 AdMob 控制台中创建的是 App Open Ad 类型的广告单元
        // 格式：ca-app-pub-[PUBLISHER_ID]/[AD_UNIT_ID]
        // 测试ID：ca-app-pub-3940256099942544/9257395921
        // 如果遇到格式错误，请检查：
        // 1. AdMob 控制台中是否创建了 App Open Ad 类型的广告单元
        // 2. 广告单元 ID 是否与代码中的完全一致（包括大小写）
        // 3. AndroidManifest 中的 APPLICATION_ID 是否正确配置
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"
        private const val AD_EXPIRY_TIME_MS = 4 * 60 * 60 * 1000L // 4 小时过期时间
    }

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Date? = null
    private var currentActivity: Activity? = null
    private var isAppInForeground = false // 应用是否在前台
    private var wasAppInBackground = false // 应用是否从后台回来
    private var hasEverBeenInBackground = false // 应用是否曾经进入过后台（用于区分冷启动）

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        Log.d(TAG, "AppOpenAdManager: 初始化完成")
    }

    /**
     * 加载广告
     * @param onAdLoaded 广告加载成功回调
     * @param onAdFailedToLoad 广告加载失败回调
     */
    fun loadAd(
        onAdLoaded: (() -> Unit)? = null,
        onAdFailedToLoad: (() -> Unit)? = null
    ) {
        // 如果正在加载或已有有效广告，则跳过
        if (isLoadingAd || isAdAvailable()) {
            Log.d(TAG, "loadAd: 跳过加载 - isLoadingAd=$isLoadingAd, isAdAvailable=${isAdAvailable()}")
            return
        }

        isLoadingAd = true
        Log.d(TAG, "loadAd: 开始加载广告 - AD_UNIT_ID=$AD_UNIT_ID")

        val request = AdRequest.Builder().build()
        // 注意：AdMob SDK 24.7.0+ 中，AppOpenAd.load 不再需要 orientation 参数
        // 广告会根据设备方向自动适配
        AppOpenAd.load(
            application,
            AD_UNIT_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "loadAd: 广告加载成功")
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date()
                    onAdLoaded?.invoke()

                    // 记录 ResponseInfo
                    val responseInfo = ad.responseInfo
                    Log.d(TAG, "loadAd: ResponseInfo - " +
                            "mediationAdapterClassName=${responseInfo?.mediationAdapterClassName}, " +
                            "responseId=${responseInfo?.responseId}, " +
                            "adapterResponses=${responseInfo?.adapterResponses?.size}")

                    // 注意：全屏内容回调会在 showAdIfAvailable 中设置，以便支持自定义回调
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "onAdFailedToLoad: 广告加载失败 - ${error.message} (code: ${error.code}, " +
                            "domain: ${error.domain}, cause: ${error.cause})")
                    
                    // 错误代码说明：
                    // 0: ERROR_CODE_INTERNAL_ERROR - 内部错误
                    // 1: ERROR_CODE_INVALID_REQUEST - 无效请求
                    // 2: ERROR_CODE_NETWORK_ERROR - 网络错误
                    // 3: ERROR_CODE_NO_FILL - 无填充（广告单元格式不匹配或没有可用广告）
                    // 8: ERROR_CODE_INVALID_AD_SIZE - 无效的广告尺寸
                    when (error.code) {
                        3 -> {
                            Log.e(TAG, "onAdFailedToLoad: 错误代码3 - 广告单元格式不匹配或无填充")
                            Log.e(TAG, "  可能原因：")
                            Log.e(TAG, "  1. 广告单元ID格式错误：$AD_UNIT_ID")
                            Log.e(TAG, "  2. AdMob控制台中未创建App Open Ad类型的广告单元")
                            Log.e(TAG, "  3. AndroidManifest中的APPLICATION_ID配置错误")
                            Log.e(TAG, "  4. 测试ID可能已过期或无效")
                            Log.e(TAG, "  建议：在AdMob控制台创建App Open Ad类型的广告单元，并使用该ID")
                        }
                    }
                    
                    isLoadingAd = false
                    appOpenAd = null
                    onAdFailedToLoad?.invoke()

                    // 记录 ResponseInfo
                    val responseInfo = error.responseInfo
                    Log.d(TAG, "onAdFailedToLoad: ResponseInfo - " +
                            "mediationAdapterClassName=${responseInfo?.mediationAdapterClassName}, " +
                            "responseId=${responseInfo?.responseId}")
                }
            }
        )
    }

    /**
     * 显示广告（如果可用）
     * @param activity 用于显示广告的 Activity
     * @param onAdDismissed 广告关闭后的回调（可选）
     * @return true 如果广告已显示，false 如果广告不可用
     */
    fun showAdIfAvailable(
        activity: Activity,
        onAdDismissed: (() -> Unit)? = null
    ): Boolean {
        Log.d(TAG, "showAdIfAvailable: 尝试显示广告 - isShowingAd=$isShowingAd, isAdAvailable=${isAdAvailable()}")

        // 如果正在显示广告，则跳过
        if (isShowingAd) {
            Log.d(TAG, "showAdIfAvailable: 广告正在显示，跳过")
            return false
        }

        // 如果广告不可用，尝试加载
        if (!isAdAvailable()) {
            Log.d(TAG, "showAdIfAvailable: 广告不可用，开始加载")
            loadAd()
            return false
        }

        // 显示广告
        val ad = appOpenAd ?: return false
        currentActivity = activity
        Log.d(TAG, "showAdIfAvailable: 显示广告")
        
        // 设置全屏内容回调（支持自定义回调）
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "onAdDismissedFullScreenContent: 广告已关闭")
                appOpenAd = null
                isShowingAd = false
                // 调用自定义回调
                onAdDismissed?.invoke()
                // 广告关闭后立即加载下一个广告
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "onAdFailedToShowFullScreenContent: 广告显示失败 - ${error.message} (code: ${error.code})")
                appOpenAd = null
                isShowingAd = false
                // 调用自定义回调
                onAdDismissed?.invoke()
                // 显示失败后加载下一个广告
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "onAdShowedFullScreenContent: 广告已显示")
                isShowingAd = true
            }

            override fun onAdImpression() {
                Log.d(TAG, "onAdImpression: 广告展示")
            }

            override fun onAdClicked() {
                Log.d(TAG, "onAdClicked: 用户点击了广告")
            }
        }
        
        ad.show(activity)
        return true
    }

    /**
     * 检查广告是否可用
     */
    private fun isAdAvailable(): Boolean {
        val ad = appOpenAd ?: return false
        val loadTime = this.loadTime ?: return false

        // 检查广告是否过期（4小时）
        val now = Date()
        val elapsedTime = now.time - loadTime.time
        val isExpired = elapsedTime >= AD_EXPIRY_TIME_MS

        if (isExpired) {
            Log.d(TAG, "isAdAvailable: 广告已过期 (elapsedTime=${elapsedTime}ms)")
            this.appOpenAd = null
            this.loadTime = null
            return false
        }

        Log.d(TAG, "isAdAvailable: 广告可用 (剩余时间=${(AD_EXPIRY_TIME_MS - elapsedTime) / 1000 / 60}分钟)")
        return true
    }

    /**
     * 设置当前 Activity（用于显示广告）
     */
    fun setCurrentActivity(activity: Activity?) {
        currentActivity = activity
        Log.d(TAG, "setCurrentActivity: 设置当前 Activity - ${activity?.javaClass?.simpleName}")
    }

    /**
     * 获取当前 Activity
     */
    fun getCurrentActivity(): Activity? = currentActivity

    // LifecycleObserver 回调
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d(TAG, "onStart: 应用进入前台 - isAppInForeground=$isAppInForeground, hasEverBeenInBackground=$hasEverBeenInBackground")
        
        // 如果应用之前在后台（isAppInForeground == false 且曾经进入过后台），标记为从后台回来
        if (!isAppInForeground && hasEverBeenInBackground) {
            wasAppInBackground = true
            Log.d(TAG, "onStart: 应用从后台回到前台")
        } else {
            wasAppInBackground = false
            Log.d(TAG, "onStart: 冷启动（首次启动）")
        }
        
        isAppInForeground = true
        
        // 只有在从后台回到前台时才显示广告（不是冷启动）
        // 冷启动时只加载广告，不显示
        if (wasAppInBackground && !isShowingAd) {
            currentActivity?.let { activity ->
                Log.d(TAG, "onStart: 从后台回来，尝试显示广告")
                showAdIfAvailable(activity)
            }
        } else {
            Log.d(TAG, "onStart: 冷启动或正在显示广告，跳过显示 - wasAppInBackground=$wasAppInBackground, isShowingAd=$isShowingAd")
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d(TAG, "onStop: 应用进入后台")
        isAppInForeground = false
        hasEverBeenInBackground = true // 标记应用曾经进入过后台
        wasAppInBackground = false
    }
}

