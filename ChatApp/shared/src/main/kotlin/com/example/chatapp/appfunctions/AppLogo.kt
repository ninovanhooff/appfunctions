/*
 * Copyright 2026 The Android Open Source Project
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
package com.example.chatapp.appfunctions

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import java.io.File

/**
 * Shares this app's launcher icon as a `content://` URI, for the `iconUrl` of an A2UI surface theme.
 *
 * The host loads the URI with its own image loader. Return the URI in an
 * [androidx.appfunctions.AppFunctionUriGrant] so the host can read it.
 */
internal object AppLogo {
    private const val IMAGE_DIR = "a2ui_images"
    private const val IMAGE_FILE = "app_logo.png"
    private const val SIZE_PX = 96

    /**
     * Writes the launcher icon to the cache once, and returns its content URI.
     *
     * @param context A context of this app.
     */
    fun contentUri(context: Context): Uri {
        val file = File(File(context.cacheDir, IMAGE_DIR).apply { mkdirs() }, IMAGE_FILE)
        if (!file.exists()) {
            val icon = context.packageManager.getApplicationIcon(context.packageName)
            file.outputStream().use { stream ->
                icon.toBitmap(SIZE_PX, SIZE_PX).compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.a2uiimages", file)
    }
}
