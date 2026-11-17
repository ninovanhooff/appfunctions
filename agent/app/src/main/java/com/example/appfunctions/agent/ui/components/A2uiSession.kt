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

import android.content.Context
import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.compose.material3.Text
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import androidx.compose.material3.a2ui.catalog.materialA2uiBasicCatalogV1
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.example.appfunctions.agent.R
import com.example.appfunctions.agent.domain.a2ui.A2uiActionOutcome
import com.example.appfunctions.agent.domain.a2ui.A2uiPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * The state of one rendered A2UI resource: its surfaces, including the data model behind input
 * fields, and the actions the user took.
 *
 * A session isn't tied to composition. A composable can leave the screen, for example when a lazy
 * list scrolls it away, and show the same session again without losing what the user typed or the
 * outcome of an action. The session runs in [scope], so an action that is in progress also
 * continues while the composable is away.
 *
 * @param payload The parsed A2UI resource.
 * @param context A context for the catalog's functions. Use the application context, since the
 *   session can outlive the activity.
 * @param scope The scope the session processes messages and runs actions in.
 */
@Stable
class A2uiSession(
    payload: A2uiPayload,
    context: Context,
    scope: CoroutineScope,
) {
    private val processor = A2uiMessageProcessor(catalogs = listOf(createCatalog(context)))

    /** The surfaces to render. */
    val surfaces = processor.activeSurfaces

    /** The actions the user took, as text. */
    val events = mutableStateListOf<String>()

    /** The errors the surfaces reported, as text. */
    val errors = mutableStateListOf<String>()

    /** Handles actions, if the host provides one. Set by the composable that shows the session. */
    var actionHandler: A2uiActionHandler? = null

    init {
        val parser = A2uiMessageParser()
        scope.launch(Dispatchers.Default) { processor.collectMessages() }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            processor.outboundEvents.collect { message ->
                when (message) {
                    is A2uiClientEventMessage -> {
                        events.add("${message.type} ${message.context}")
                        val handler = actionHandler
                        if (handler != null) {
                            // Don't block the collector while the action runs.
                            launch {
                                val outcome = handler(message.type, message.context)
                                if (outcome != null) {
                                    processor.processInput(
                                        parser,
                                        outcomeMessage(message.surfaceId, outcome),
                                    )
                                }
                            }
                        }
                    }
                    is A2uiClientErrorMessage -> errors.add("${message.code}: ${message.message}")
                }
            }
        }
        payload.messages.forEach { processor.processInput(parser, it) }
    }
}

/**
 * Keeps [A2uiSession]s alive beyond the composition that shows them, for example in a ViewModel,
 * so that a resource in a lazy list keeps its state when it scrolls off screen.
 *
 * @param scope The scope the sessions run in.
 */
class A2uiSessionCache(private val scope: CoroutineScope) {
    private class Entry(val content: String, val session: A2uiSession)

    private val entries = HashMap<String, Entry>()

    /**
     * Returns the session for [key], creating it if there isn't one for this [content] yet.
     *
     * @param key Identifies the resource, such as its message and position in the message.
     * @param content The content of the resource.
     * @param payload The parsed [content].
     * @param context A context for the catalog's functions.
     */
    fun getOrCreate(
        key: String,
        content: String,
        payload: A2uiPayload,
        context: Context,
    ): A2uiSession {
        val existing = entries[key]
        if (existing != null && existing.content == content) return existing.session
        return A2uiSession(payload, context.applicationContext, scope)
            .also { entries[key] = Entry(content, it) }
    }
}

private fun createCatalog(context: Context) =
    materialA2uiBasicCatalogV1(
        image =
            MaterialA2uiBasicCatalogV1Defaults.image { url, description, scale, modifier, onError ->
                AsyncImage(
                    model = url,
                    contentDescription = description,
                    contentScale = scale,
                    modifier = modifier,
                    onError = { state -> onError(state.result.throwable) },
                )
            },
        video =
            MaterialA2uiBasicCatalogV1Defaults.video { _, modifier, _ ->
                Text(stringResource(R.string.a2ui_media_unsupported), modifier)
            },
        audioPlayer =
            MaterialA2uiBasicCatalogV1Defaults.audioPlayer { _, _, modifier, _ ->
                Text(stringResource(R.string.a2ui_media_unsupported), modifier)
            },
        functions = A2uiBasicCatalogV1.Functions(context),
    )

/** Builds an A2UI message that replaces the surface's card content with [outcome]. */
private fun outcomeMessage(
    surfaceId: String,
    outcome: A2uiActionOutcome,
): String {
    val children = JSONArray().put("outcome_message")
    val components =
        JSONArray()
            .put(JSONObject().put("id", "root").put("component", "Card").put("child", "outcome"))
            .put(
                JSONObject()
                    .put("id", "outcome_message")
                    .put("component", "Text")
                    .put("text", outcome.message)
                    .put("variant", "h4"),
            )
    if (outcome.body != null) {
        children.put("outcome_body")
        components.put(
            JSONObject()
                .put("id", "outcome_body")
                .put("component", "Text")
                .put("text", outcome.body)
                .put("variant", "body"),
        )
    }
    components.put(
        JSONObject().put("id", "outcome").put("component", "Column").put("children", children),
    )
    return JSONObject()
        .put("version", "v0.9")
        .put(
            "updateComponents",
            JSONObject().put("surfaceId", surfaceId).put("components", components),
        ).toString()
}
