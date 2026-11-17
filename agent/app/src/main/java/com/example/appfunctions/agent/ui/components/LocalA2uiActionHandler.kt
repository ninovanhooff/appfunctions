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

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.appfunctions.agent.domain.a2ui.A2uiActionOutcome

/**
 * Handles an action sent by a button on an A2UI surface, for example by calling an AppFunction.
 *
 * Receives the action name and its context. Returns the outcome, which the renderer shows in place
 * of the surface's card, or null if the action isn't handled.
 */
typealias A2uiActionHandler = suspend (name: String, context: Map<String, Any?>) -> A2uiActionOutcome?

/** The handler for actions on A2UI surfaces, or null to only list them below the surface. */
val LocalA2uiActionHandler = staticCompositionLocalOf<A2uiActionHandler?> { null }
