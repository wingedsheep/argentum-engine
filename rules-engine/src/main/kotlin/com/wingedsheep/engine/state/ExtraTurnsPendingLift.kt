package com.wingedsheep.engine.state

import com.wingedsheep.engine.state.components.player.ExtraTurnsComponent
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * Reads games persisted while [ExtraTurnsComponent.pending] was a count of the extra turns that
 * hadn't begun yet; it is now one entry per turn, carrying that turn's riders. A count of N becomes
 * N rider-less turns — exactly right for those games, whose riders (a lose clause, Kang's power-up
 * lockout) were already applied when each turn was created.
 */
internal object ExtraTurnsPendingLift {
    private val COMPONENT = ExtraTurnsComponent::class.java.name

    fun lift(element: JsonElement): JsonElement {
        val state = element as? JsonObject ?: return element
        val entities = state["entities"] as? JsonObject ?: return state
        var changed = false
        val lifted = entities.mapValues { (_, container) ->
            val components = container as? JsonObject ?: return@mapValues container
            val component = components[COMPONENT] as? JsonObject ?: return@mapValues container
            val count = (component["pending"] as? JsonPrimitive)?.intOrNull ?: return@mapValues container
            changed = true
            val turns = JsonArray(List(count.coerceAtLeast(0)) { JsonObject(emptyMap()) })
            JsonObject(components + (COMPONENT to JsonObject(component + ("pending" to turns))))
        }
        return if (changed) JsonObject(state + ("entities" to JsonObject(lifted))) else state
    }
}
