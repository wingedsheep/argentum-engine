package com.wingedsheep.engine.state

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer

/** A snapshot uses execution fields that this reader cannot safely restore. */
class UnsupportedGameStateFormatException(message: String) : SerializationException(message)

/**
 * Reject obsolete execution fields before permissive JSON decoding can discard them, lift the
 * pre-record flat trigger facts into their `triggerContext` record ([LegacyTriggerContextLift]), and
 * rewrite retired SDK types to their current shape ([RetiredSdkTypeLift]).
 */
@OptIn(ExperimentalSerializationApi::class)
object GameStateSerializer : JsonTransformingSerializer<GameState>(GameState.generatedSerializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val state = element as? JsonObject ?: return element
        if ("pendingDecision" in state) {
            throw UnsupportedGameStateFormatException("Unsupported GameState format: root pendingDecision")
        }
        val stack = state["continuationStack"] as? JsonArray
        if (stack?.any { it is JsonObject && "decisionId" in it } == true) {
            throw UnsupportedGameStateFormatException("Unsupported GameState format: continuation decisionId")
        }
        // Trigger facts used to be flat fields on each carrier; move them into the nested record
        // before permissive decoding (ignoreUnknownKeys) can discard them.
        return RetiredSdkTypeLift.lift(LegacyTriggerContextLift.lift(state))
    }
}
