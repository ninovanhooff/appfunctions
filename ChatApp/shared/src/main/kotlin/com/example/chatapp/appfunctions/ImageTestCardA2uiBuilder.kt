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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Builds an A2UI card that shows the same kind of image delivered in three ways, to find out which
 * ones a host can render:
 * - an `https` URL the host downloads itself,
 * - a `content://` URI from this app's FileProvider, which the caller can read through an
 *   [androidx.appfunctions.AppFunctionUriGrant],
 * - a `data:` URI with the PNG bytes inline as base64.
 */
internal object ImageTestCardA2uiBuilder {
    private const val VERSION = "v0.9"
    private const val BASIC_CATALOG_ID = "https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json"
    private const val SURFACE_ID = "image_test_card"
    private const val IMAGE_DIR = "a2ui_images"
    private const val IMAGE_FILE = "test_image.png"
    private const val IMAGE_SIZE_PX = 96

    /** An image the host downloads over the network: the app logo. */
    private const val HTTPS_IMAGE_URL = APP_LOGO_URL

    /**
     * The card and the content URI it shows.
     *
     * @property a2uiJson The A2UI messages, as a JSON array.
     * @property contentUris The `content://` URIs the card shows, which the caller needs read access to.
     */
    class Result(
        val a2uiJson: String,
        val contentUris: List<Uri>,
    )

    /**
     * Writes the test image to the cache, and builds the card.
     *
     * @param context A context of this app, used for the cache and the FileProvider.
     */
    fun build(context: Context): Result {
        val png = drawTestImage()
        val contentUri = writeToCache(context, png)
        val dataUri = "data:image/png;base64," + Base64.encodeToString(png, Base64.NO_WRAP)

        val components =
            JSONArray()
                .put(component("root", "Card").put("child", "content"))
                .put(column("content", "title", "row_https", "row_content", "row_data"))
                .put(text("title", "Image loading test", "h4"))
                .put(imageRow("row_https"))
                .put(text("row_https_label", "https URL", "body"))
                .put(image("row_https_image", HTTPS_IMAGE_URL, "App icon over https"))
                .put(imageRow("row_content"))
                .put(text("row_content_label", "content:// URI", "body"))
                .put(image("row_content_image", contentUri.toString(), "Test image over content URI"))
                .put(imageRow("row_data"))
                .put(text("row_data_label", "data: URI (${png.size} bytes)", "body"))
                .put(image("row_data_image", dataUri, "Test image as data URI"))

        val createSurface =
            JSONObject()
                .put("version", VERSION)
                .put(
                    "createSurface",
                    JSONObject()
                        .put("surfaceId", SURFACE_ID)
                        .put("catalogId", BASIC_CATALOG_ID)
                        .put(
                            "theme",
                            JSONObject()
                                .put("agentDisplayName", "ChatApp")
                                .put("iconUrl", APP_LOGO_URL),
                        ),
                )
        val updateComponents =
            JSONObject()
                .put("version", VERSION)
                .put(
                    "updateComponents",
                    JSONObject()
                        .put("surfaceId", SURFACE_ID)
                        .put("components", components),
                )
        return Result(
            a2uiJson = JSONArray().put(createSurface).put(updateComponents).toString(),
            contentUris = listOf(contentUri),
        )
    }

    /** Draws a small PNG, so the test needs no bundled asset and no network. */
    private fun drawTestImage(): ByteArray {
        val bitmap = Bitmap.createBitmap(IMAGE_SIZE_PX, IMAGE_SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(0x1A, 0x73, 0xE8)
        canvas.drawCircle(IMAGE_SIZE_PX / 2f, IMAGE_SIZE_PX / 2f, IMAGE_SIZE_PX / 2f, paint)
        paint.color = Color.WHITE
        paint.textSize = IMAGE_SIZE_PX / 3f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("A2UI", IMAGE_SIZE_PX / 2f, IMAGE_SIZE_PX / 2f + paint.textSize / 3f, paint)
        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        }
    }

    private fun writeToCache(
        context: Context,
        png: ByteArray,
    ): Uri {
        val dir = File(context.cacheDir, IMAGE_DIR).apply { mkdirs() }
        val file = File(dir, IMAGE_FILE).apply { writeBytes(png) }
        return FileProvider.getUriForFile(context, "${context.packageName}.a2uiimages", file)
    }

    /** A row with a label on the left and the image on the right. */
    private fun imageRow(id: String): JSONObject =
        component(id, "Row")
            .put("align", "center")
            .put("children", JSONArray().put("${id}_image").put("${id}_label"))

    private fun image(
        id: String,
        url: String,
        description: String,
    ): JSONObject =
        component(id, "Image")
            .put("url", url)
            .put("description", description)
            .put("fit", "contain")
            .put("variant", "smallFeature")

    private fun component(
        id: String,
        type: String,
    ): JSONObject = JSONObject().put("id", id).put("component", type)

    private fun column(
        id: String,
        vararg children: String,
    ): JSONObject = component(id, "Column").put("children", JSONArray(children.toList()))

    private fun text(
        id: String,
        text: String,
        variant: String,
    ): JSONObject = component(id, "Text").put("text", text).put("variant", variant)
}
