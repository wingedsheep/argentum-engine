package com.wingedsheep.engine.state

import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.components.player.ExtraTurnsComponent
import com.wingedsheep.engine.state.components.player.PendingExtraTurn
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Games persisted while [ExtraTurnsComponent.pending] was a count must still load: [ExtraTurnsPendingLift]
 * (run by [GameStateSerializer]) turns a count of N into N rider-less pending turns.
 */
class ExtraTurnsPendingLiftTest : FunSpec({
    val json = Json {
        serializersModule = engineSerializersModule
        allowStructuredMapKeys = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    val player = EntityId("player-1")
    val componentKey = ExtraTurnsComponent::class.java.name

    fun stateWith(component: ExtraTurnsComponent) =
        GameState().withEntity(player, ComponentContainer.of(component))

    fun withPendingCount(encoded: JsonObject, count: Int): JsonObject {
        val entities = encoded.getValue("entities").jsonObject
        val container = entities.getValue(player.value).jsonObject
        val component = container.getValue(componentKey).jsonObject
        val old = JsonObject(component + ("pending" to JsonPrimitive(count)))
        return JsonObject(encoded + ("entities" to JsonObject(entities + (player.value to JsonObject(container + (componentKey to old))))))
    }

    test("a counted pending becomes that many rider-less extra turns") {
        val encoded = json.parseToJsonElement(json.encodeToString(stateWith(ExtraTurnsComponent(begun = 1)))).jsonObject
        val decoded = json.decodeFromString<GameState>(withPendingCount(encoded, 2).toString())
        decoded.getEntity(player)?.get<ExtraTurnsComponent>() shouldBe
            ExtraTurnsComponent(pending = listOf(PendingExtraTurn(), PendingExtraTurn()), begun = 1)
    }

    test("current-format extra turns round-trip untouched") {
        val current = stateWith(ExtraTurnsComponent(pending = listOf(PendingExtraTurn(loseAtEndStep = true)), begun = 0))
        json.decodeFromString<GameState>(json.encodeToString(current)).getEntity(player)?.get<ExtraTurnsComponent>() shouldBe
            current.getEntity(player)?.get<ExtraTurnsComponent>()
    }
})
