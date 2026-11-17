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
package com.example.appfunctions.agent.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.a2ui.A2uiSurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appfunctions.agent.R
import com.example.appfunctions.agent.domain.a2ui.A2uiPayload
import com.example.appfunctions.agent.domain.a2ui.parseA2uiContent
import com.example.appfunctions.agent.ui.theme.GoogleSansCodeFontFamily

/** The tallest a surface can be, which also gives lazy Basic Catalog lists bounded height. */
private val MaxSurfaceHeight = 480.dp

/**
 * Renders an A2UI resource returned by an AppFunction, as a host such as Gemini might.
 *
 * Only the A2UI Basic Catalog is supported. The renderer reports other catalogs as errors. User
 * actions are listed below the surface, and also passed to [LocalA2uiActionHandler] when one is
 * provided. The outcome it returns replaces the surface's content.
 *
 * @param content The content of the A2UI resource.
 * @param modifier The modifier to apply to the preview.
 * @param sessionKey Identifies the resource in the [LocalA2uiSessionCache]. With a key and a cache,
 *   the resource keeps its state, such as edited text, when it leaves the composition. Without
 *   either, its state is lost then.
 */
@Composable
fun A2uiResourceRenderer(
    content: String,
    modifier: Modifier = Modifier,
    sessionKey: String? = null,
) {
    val payload = remember(content) { parseA2uiContent(content) }
    var showRawJson by rememberSaveable(content) { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = payload.agentDisplayName ?: stringResource(R.string.a2ui_preview_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (payload.error != null) {
                Text(
                    text = payload.error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                A2uiSurfaces(payload, content, sessionKey)
            }

            TextButton(onClick = { showRawJson = !showRawJson }) {
                Text(
                    stringResource(
                        if (showRawJson) R.string.a2ui_hide_json else R.string.a2ui_show_json,
                    ),
                )
            }
            if (showRawJson) {
                SelectionContainer {
                    Text(
                        text = content,
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                fontFamily = GoogleSansCodeFontFamily,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun A2uiSurfaces(
    payload: A2uiPayload,
    content: String,
    sessionKey: String?,
) {
    val cache = LocalA2uiSessionCache.current
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val session =
        remember(content, sessionKey, cache) {
            if (cache != null && sessionKey != null) {
                cache.getOrCreate(sessionKey, content, payload, context)
            } else {
                // The session ends with this composable, as there's nowhere to keep it.
                A2uiSession(payload, context, scope)
            }
        }
    val actionHandler = LocalA2uiActionHandler.current
    SideEffect { session.actionHandler = actionHandler }
    val surfaces by session.surfaces.collectAsStateWithLifecycle()
    val events = session.events
    val errors = session.errors

    MaterialTheme(colorScheme = brandedColorScheme(payload.primaryColor)) {
        surfaces.forEach { surface ->
            key(surface.id) {
                A2uiSurface(
                    surfaceModel = surface,
                    modifier = Modifier.fillMaxWidth().heightIn(max = MaxSurfaceHeight),
                )
            }
        }
    }

    errors.forEach { error ->
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
    events.forEach { event ->
        Text(
            text = stringResource(R.string.a2ui_action_sent, event),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Applies a brand color to primary elements only. The Connected Apps UI guidelines reserve brand
 * colors for elements such as the primary call to action, and leave everything else to the host.
 */
@Composable
private fun brandedColorScheme(primaryColor: String?) =
    MaterialTheme.colorScheme.let { scheme ->
        val primary =
            remember(primaryColor) {
                primaryColor?.let { runCatching { Color(it.toColorInt()) }.getOrNull() }
            }
        if (primary == null) {
            scheme
        } else {
            scheme.copy(
                primary = primary,
                onPrimary = if (primary.luminance() > 0.5f) Color.Black else Color.White,
            )
        }
    }
