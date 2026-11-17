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
package com.example.appfunctions.agent.domain.a2ui

import androidx.appfunctions.metadata.AppFunctionMetadata
import com.example.appfunctions.agent.domain.appfunction.AppFunctionExceptionFormatter
import com.example.appfunctions.agent.domain.appfunction.ConvertInputToAppFunctionDataUseCase
import com.example.appfunctions.agent.domain.appfunction.ExecuteAppFunctionResult
import com.example.appfunctions.agent.domain.appfunction.ExecuteAppFunctionUseCase
import javax.inject.Inject

/** The A2UI action name sent by the Send button of a message card. */
const val A2UI_SEND_MESSAGE_ACTION = "send_message"

private const val SEND_MESSAGE_FUNCTION = "sendMessage"

/**
 * What to show in an A2UI surface after an action ran.
 *
 * @property message A short description of the outcome, such as the confirmation or the error.
 * @property body The content the action acted on, such as the message that was sent, if any.
 */
data class A2uiActionOutcome(
    val message: String,
    val body: String? = null,
)

/** Maps actions from A2UI surfaces to AppFunction calls. */
class HandleA2uiActionUseCase
    @Inject
    constructor(
        private val convertInputToAppFunctionDataUseCase: ConvertInputToAppFunctionDataUseCase,
        private val executeAppFunctionUseCase: ExecuteAppFunctionUseCase,
    ) {
        /**
         * Handles an action from an A2UI surface. A `send_message` action calls `sendMessage` with
         * the recipient and message carried in the action context.
         *
         * @param name The action name.
         * @param context The action context.
         * @param functions The functions the action may call.
         * @param threadId Optional chat thread ID used for interaction attribution.
         * @return The outcome to show in the surface, or null if the action isn't handled.
         */
        suspend operator fun invoke(
            name: String,
            context: Map<String, Any?>,
            functions: List<AppFunctionMetadata>,
            threadId: String? = null,
        ): A2uiActionOutcome? {
            if (name != A2UI_SEND_MESSAGE_ACTION) return null
            val sendFunction =
                functions.firstOrNull { it.id.endsWith(SEND_MESSAGE_FUNCTION) }
                    ?: return A2uiActionOutcome("No sendMessage function found")
            val endpointValue = context["endpointValue"] as? String
            val messageBody = context["messageBody"] as? String
            if (endpointValue == null || messageBody == null) {
                return A2uiActionOutcome("Action is missing its message")
            }

            val parameters =
                convertInputToAppFunctionDataUseCase(
                    parameters = sendFunction.parameters,
                    components = sendFunction.components,
                    inputs = mapOf("endpointValue" to endpointValue, "messageBody" to messageBody),
                ).getOrElse { return A2uiActionOutcome(it.message ?: "Failed to convert input") }
            return when (val result = executeAppFunctionUseCase(sendFunction, parameters, threadId)) {
                is ExecuteAppFunctionResult.Data ->
                    A2uiActionOutcome(
                        message = result.formattedJson.trim().removeSurrounding("\""),
                        body = messageBody,
                    )
                is ExecuteAppFunctionResult.PendingIntentAction -> A2uiActionOutcome("Action required")
                is ExecuteAppFunctionResult.Error ->
                    A2uiActionOutcome(
                        AppFunctionExceptionFormatter.getAppFunctionException(result.exception)
                            ?.let { AppFunctionExceptionFormatter.format(it, sendFunction.id) }
                            ?: result.exception.message
                            ?: "Unknown error",
                    )
            }
        }
    }
