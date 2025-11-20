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
    
    companion object {
        private const val TAG = "NavActivity"
        // 测试设备 ID 列表 - 请替换为您的实际测试设备 ID
        // 获取方式：运行应用后查看 logcat，搜索 "To get test ads on this device"
        private val TEST_DEVICE_IDS = listOf(
            "YOUR_TEST_DEVICE_ID_HERE" // 示例：替换为实际设备 ID
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // 初始化 Google Mobile Ads SDK
        initializeMobileAds()
        
        // 初始化合规授权管理器并收集用户同意
        initializeConsent()
        
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
                            val bundle = bundleOf("userId" to it)
                            findNavController().navigate(R.id.nav_profile, bundle)
                            scope.launch {
                                drawerState.close()
                            }
                            selectedMenu = it
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
     * See https://issuetracker.google.com/142847973
     */
    private fun findNavController(): NavController {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController
    }
    
    /**
     * 初始化 Google Mobile Ads SDK
     * 注意：应该在用户同意后调用
     */
    private fun initializeMobileAds() {
        Log.d(TAG, "initializeMobileAds: start")
        
        // 配置测试设备
        val requestConfiguration = RequestConfiguration.Builder()
            .setTestDeviceIds(TEST_DEVICE_IDS)
            .build()
        MobileAds.setRequestConfiguration(requestConfiguration)
        Log.d(TAG, "RequestConfiguration set with test devices: $TEST_DEVICE_IDS")
        
        // 初始化 MobileAds SDK
        MobileAds.initialize(this) { initializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            Log.d(TAG, "MobileAds initialization completed")
            statusMap.forEach { (adapter, status) ->
                Log.d(TAG, "Adapter: $adapter, State: ${status.initializationState}, " +
                        "Description: ${status.description}")
            }
        }
    }
    
    /**
     * 初始化合规授权管理器
     * 只有在 canRequestAds 为 true 时才初始化 MobileAds SDK
     */
    private fun initializeConsent() {
        Log.d(TAG, "initializeConsent: start")
        val consentManager = GoogleMobileAdsConsentManager.getInstance(this)
        
        // 收集用户同意信息
        // 注意：isDebugGeography 在生产环境中应设置为 false
        consentManager.gatherConsent(
            activity = this,
            testDeviceIds = TEST_DEVICE_IDS,
            isDebugGeography = false, // 生产环境设为 false
            onConsentGathered = { canRequestAds ->
                Log.d(TAG, "Consent gathered, canRequestAds: $canRequestAds")
                if (canRequestAds) {
                    // 只有在可以请求广告时才初始化 MobileAds SDK
                    initializeMobileAds()
                } else {
                    Log.w(TAG, "Cannot request ads, MobileAds SDK not initialized")
                }
                
                // 如果需要显示隐私选项，可以在这里添加按钮或菜单项
                if (consentManager.isPrivacyOptionsRequired) {
                    Log.d(TAG, "Privacy options are required. Consider showing a button to open them.")
                }
            }
        )
    }
}
