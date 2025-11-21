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

package com.example.compose.jetchat

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.DrawerValue.Closed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.compose.jetchat.ads.GoogleMobileAdsConsentManager
import com.example.compose.jetchat.ads.InterstitialAdManager
import com.example.compose.jetchat.components.JetchatDrawer
import com.example.compose.jetchat.databinding.ContentMainBinding
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kotlinx.coroutines.launch

/**
 * Main activity for the app.
 */
class NavActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    
    // AdMob 相关管理器
    private val consentManager = GoogleMobileAdsConsentManager.getInstance()
    private val interstitialAdManager = InterstitialAdManager.getInstance()
    
    // 用于跟踪 MobileAds SDK 是否已初始化
    private var isMobileAdsInitialized = false

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // 初始化 MobileAds SDK（每个应用会话只初始化一次）
        initializeMobileAds()
        
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets -> insets }

        setContentView(
            ComposeView(this).apply {
                consumeWindowInsets = false
                setContent {
                    val drawerState = rememberDrawerState(initialValue = Closed)
                    val drawerOpen by viewModel.drawerShouldBeOpened
                        .collectAsStateWithLifecycle()

                    var selectedMenu by remember { mutableStateOf("composers") }
                    if (drawerOpen) {
                        // Open drawer and reset state in VM.
                        LaunchedEffect(Unit) {
                            // wrap in try-finally to handle interruption whiles opening drawer
                            try {
                                drawerState.open()
                            } finally {
                                viewModel.resetOpenDrawerAction()
                            }
                        }
                    }

                    val scope = rememberCoroutineScope()

                    JetchatDrawer(
                        drawerState = drawerState,
                        selectedMenu = selectedMenu,
                        onChatClicked = {
                            findNavController().popBackStack(R.id.nav_home, false)
                            scope.launch {
                                drawerState.close()
                            }
                            selectedMenu = it
                        },
                        onProfileClicked = {
                            val userId = it
                            // 在导航到个人资料页面之前，尝试显示插页式广告
                            // 如果广告未准备好，则继续正常导航流程
                            interstitialAdManager.showAd(
                                this@NavActivity,
                                onAdDismissed = {
                                    // 广告关闭后执行导航
                                    val bundle = bundleOf("userId" to userId)
                                    findNavController().navigate(R.id.nav_profile, bundle)
                                }
                            )
                            // 如果广告未准备好，showAd 会立即执行回调，导航会正常进行
                            scope.launch {
                                drawerState.close()
                            }
                            selectedMenu = userId
                        },
                    ) {
                        AndroidViewBinding(ContentMainBinding::inflate)
                    }
                }
            },
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController().navigateUp() || super.onSupportNavigateUp()
    }

    /**
     * 初始化 MobileAds SDK
     * 每个应用会话只初始化一次
     */
    private fun initializeMobileAds() {
        if (isMobileAdsInitialized) {
            Log.d("NavActivity", "MobileAds SDK 已初始化，跳过重复初始化")
            return
        }
        
        // 配置测试设备 ID（用于测试广告）
        // TODO: 替换为您的测试设备 ID
        // 获取测试设备 ID 的方法：运行应用后查看 logcat，搜索 "Use RequestConfiguration.Builder().setTestDeviceIds"
        val testDeviceIds: List<String> = listOf(
            // "YOUR_TEST_DEVICE_ID_HERE" // 示例：从 logcat 中获取
        )
        val requestConfiguration = RequestConfiguration.Builder()
            .setTestDeviceIds(testDeviceIds)
            .build()
        MobileAds.setRequestConfiguration(requestConfiguration)
        
        // 初始化 MobileAds SDK
        MobileAds.initialize(this) { initializationStatus ->
            val adapterStatusMap = initializationStatus.adapterStatusMap
            for (adapterClass in adapterStatusMap.keys) {
                val status = adapterStatusMap[adapterClass]
                Log.d(
                    "NavActivity",
                    "广告适配器: $adapterClass, 状态: ${status?.initializationState}, " +
                        "描述: ${status?.description}"
                )
            }
            
            // 记录 SDK 版本
            Log.d("NavActivity", "MobileAds SDK 版本: ${MobileAds.getVersion()}")
            
            isMobileAdsInitialized = true
            
            // SDK 初始化完成后，初始化同意管理器
            initializeConsentManager()
        }
    }
    
    /**
     * 初始化同意管理器
     */
    private fun initializeConsentManager() {
        // TODO: 在生产环境中，将 isDebug 设置为 false
        // 在测试环境中，可以设置为 true 并配置测试设备 ID
        val isDebug = true // 仅用于测试，生产环境应设置为 false
        val testDeviceId: String? = null // TODO: 如果需要测试同意流程，提供测试设备 ID
        
        consentManager.initialize(this, isDebug, testDeviceId)
        
        // 等待同意流程完成后加载广告
        // 注意：实际应用中，应该在同意流程完成后再加载广告
        // 这里使用延迟来模拟，实际应该通过回调来处理
        window.decorView.postDelayed({
            if (consentManager.canLoadAds()) {
                interstitialAdManager.loadAd(this, consentManager)
            }
        }, 2000) // 延迟 2 秒，等待同意流程完成
    }
    
    /**
     * 显示插页式广告（在合适的业务触发点调用）
     * 
     * 示例触发点：
     * - 用户完成某个操作后
     * - 导航到新页面时
     * - 应用从后台恢复时
     * 
     * @param onAdDismissed 广告关闭后的回调（可选）
     */
    fun showInterstitialAd(onAdDismissed: (() -> Unit)? = null) {
        if (interstitialAdManager.showAd(this, onAdDismissed)) {
            Log.d("NavActivity", "插页式广告已显示")
        } else {
            Log.d("NavActivity", "插页式广告未准备好，继续正常流程")
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 在应用恢复时，可以尝试加载广告（如果还未加载）
        if (isMobileAdsInitialized && consentManager.canLoadAds() && !interstitialAdManager.isAdReady()) {
            interstitialAdManager.loadAd(this, consentManager)
        }
    }
    
    /**
     * See https://issuetracker.google.com/142847973
     */
    private fun findNavController(): NavController {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController
    }
}
