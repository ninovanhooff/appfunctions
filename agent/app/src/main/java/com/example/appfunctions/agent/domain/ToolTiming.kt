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
package com.example.appfunctions.agent.domain

/**
 * How long one AppFunction call took, from the request until the response arrived. Kept in memory
 * only, so timings disappear when the app process restarts.
 *
 * @property finishedAtMillis Wall-clock time the call finished; used to place it in the chat.
 */
data class ToolTiming(
    val id: String,
    val threadId: String,
    val functionId: String,
    val packageName: String,
    val durationMillis: Long,
    val finishedAtMillis: Long,
    val succeeded: Boolean,
    /** The arguments the model passed, one `name: value` per line. */
    val arguments: String = "",
    /** What the function returned, or the error it failed with. */
    val response: String = "",
)
