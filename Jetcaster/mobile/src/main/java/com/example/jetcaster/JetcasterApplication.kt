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

package com.example.jetcaster

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application which sets up our dependency [Graph] with a context.
 */
@HiltAndroidApp
class JetcasterApplication :
    Application(),
    ImageLoaderFactory {

    @Inject lateinit var imageLoader: ImageLoader

    override fun onCreate() {
        super.onCreate()
        initializeMobileAds()
    }

    private fun initializeMobileAds() {
        // 注册测试设备 ID（用于开发测试）
        val testDeviceIds: List<String> = listOf(
            // 添加你的测试设备 ID，可以通过 logcat 查看
            // "YOUR_TEST_DEVICE_ID_HERE"
        )
        val requestConfiguration = RequestConfiguration.Builder()
            .setTestDeviceIds(testDeviceIds)
            .build()
        MobileAds.setRequestConfiguration(requestConfiguration)

        // 初始化 MobileAds SDK
        MobileAds.initialize(this) { initializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for (adapterClass in statusMap.keys) {
                val status = statusMap[adapterClass]
                Log.d(
                    TAG,
                    "AdMob初始化 - 适配器: $adapterClass, 状态: ${status?.initializationState}, " +
                        "描述: ${status?.description}",
                )
            }
            Log.d(TAG, "AdMob SDK 初始化完成")
        }
    }

    override fun newImageLoader(): ImageLoader = imageLoader

    companion object {
        private const val TAG = "JetcasterApplication"
    }
}
