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
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.DefaultLifecycleObserver
import com.example.jetlagged.ads.AppOpenAdManager

/**
 * 自定义 Application
 * 实现 ActivityLifecycleCallbacks 和 ProcessLifecycleObserver
 * 管理 App Open Ad 的生命周期
 */
class MyApplication : Application(), Application.ActivityLifecycleCallbacks {
    companion object {
        private const val TAG = "AdMob_App"
        lateinit var instance: MyApplication
            private set
    }

    lateinit var appOpenAdManager: AppOpenAdManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "onCreate: Application 创建")

        // 初始化 AppOpenAdManager
        appOpenAdManager = AppOpenAdManager(this)

        // 注册 Activity 生命周期回调
        registerActivityLifecycleCallbacks(this)

        // 注册 ProcessLifecycleOwner 观察者
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                Log.d(TAG, "onStart: 应用进程进入前台")
            }

            override fun onStop(owner: LifecycleOwner) {
                super.onStop(owner)
                Log.d(TAG, "onStop: 应用进程进入后台")
            }
        })
        Log.d(TAG, "onCreate: Application 初始化完成")
    }

    // ActivityLifecycleCallbacks 实现
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Log.d(TAG, "onActivityCreated: ${activity.javaClass.simpleName}")
    }

    override fun onActivityStarted(activity: Activity) {
        Log.d(TAG, "onActivityStarted: ${activity.javaClass.simpleName}")
        appOpenAdManager.setCurrentActivity(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        Log.d(TAG, "onActivityResumed: ${activity.javaClass.simpleName}")
        appOpenAdManager.setCurrentActivity(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        Log.d(TAG, "onActivityPaused: ${activity.javaClass.simpleName}")
    }

    override fun onActivityStopped(activity: Activity) {
        Log.d(TAG, "onActivityStopped: ${activity.javaClass.simpleName}")
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        Log.d(TAG, "onActivitySaveInstanceState: ${activity.javaClass.simpleName}")
    }

    override fun onActivityDestroyed(activity: Activity) {
        Log.d(TAG, "onActivityDestroyed: ${activity.javaClass.simpleName}")
        if (appOpenAdManager.getCurrentActivity() == activity) {
            appOpenAdManager.setCurrentActivity(null)
        }
    }
}

