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

package com.example.reply.ui

import android.app.Activity
import android.app.Application
import android.util.Log

private const val TAG = "OpenAdLifecycle"

class ReplyApplication : Application() {

    val appOpenAdManager: AppOpenAdManager by lazy {
        Log.d(TAG, "ReplyApplication: Initializing AppOpenAdManager")
        AppOpenAdManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ReplyApplication: onCreate")
        appOpenAdManager
    }

    fun showAdIfAvailable(activity: Activity, onAdDismissed: (() -> Unit)? = null) {
        appOpenAdManager.setCurrentActivity(activity)
        appOpenAdManager.showAdIfAvailable(activity, onAdDismissed)
    }
}

