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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 原生广告使用示例
 *
 * 这个文件展示了如何在 Compose 中使用原生广告组件。
 * 您可以在任何 Compose 屏幕中按照以下方式使用：
 *
 * @example
 * ```
 * Column {
 *     // 您的内容
 *     Text("您的内容")
 *
 *     // 原生广告
 *     NativeAdComposable(
 *         modifier = Modifier
 *             .fillMaxWidth()
 *             .padding(16.dp),
 *         onAdLoaded = {
 *             // 广告加载成功时的回调
 *         },
 *         onAdFailedToLoad = { error ->
 *             // 广告加载失败时的回调
 *         }
 *     )
 *
 *     // 更多内容
 *     Text("更多内容")
 * }
 * ```
 */
@Composable
fun NativeAdUsageExample() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("这是您的内容")

        // 原生广告示例
        NativeAdComposable(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            onAdLoaded = {
                // 广告加载成功
            },
            onAdFailedToLoad = { error ->
                // 广告加载失败，可以记录日志或显示错误信息
            }
        )

        Text("更多内容")
    }
}

