package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.engine.core.AbilityCounteredEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class CounteredAbilityEventTest : FunSpec({
    val json = Json {
        serializersModule = engineSerializersModule
        encodeDefaults = true
    }

    test("countered ability retains source metadata after its source has disappeared") {
        val abilityId = EntityId.of("countered-ability")
        val sourceId = EntityId.of("departed-source")
        val controllerId = EntityId.of("ability-controller")
        val ability = TriggeredAbilityOnStackComponent(
            sourceId = sourceId,
            sourceName = "Departed Source",
            controllerId = controllerId,
            effect = Effects.DrawCards(1),
            description = "Draw a card",
        )
        val state = GameState(
            entities = mapOf(abilityId to ComponentContainer.of(ability)),
            stack = listOf(abilityId),
        )

        // The source entity is already gone. The ability's stack component is the authoritative
        // last-known relationship, and countering is the final chance to capture it.
        state.getEntity(sourceId) shouldBe null

        val result = StackResolver(cardRegistry = CardRegistry()).counterAbility(state, abilityId)

        result.state.stack.shouldBeEmpty()
        val event = result.events.filterIsInstance<AbilityCounteredEvent>().single()
        event.sourceId shouldBe sourceId
        event.sourceName shouldBe "Departed Source"
        event.controllerId shouldBe controllerId

        val encoded = json.parseToJsonElement(json.encodeToString<GameEvent>(event)).jsonObject
        encoded["sourceId"]?.jsonPrimitive?.content shouldBe sourceId.value
        encoded["sourceName"]?.jsonPrimitive?.content shouldBe "Departed Source"
        encoded["controllerId"]?.jsonPrimitive?.content shouldBe controllerId.value
    }

    test("activated ability uses its stack controller rather than the source's current controller") {
        val abilityId = EntityId.of("activated-ability")
        val sourceId = EntityId.of("changed-controller-source")
        val abilityControllerId = EntityId.of("ability-controller")
        val currentSourceControllerId = EntityId.of("new-source-controller")
        val ability = ActivatedAbilityOnStackComponent(
            sourceId = sourceId,
            sourceName = "Changed Source",
            controllerId = abilityControllerId,
            effect = Effects.DrawCards(1),
        )
        val state = GameState(
            entities = mapOf(
                sourceId to ComponentContainer.of(ControllerComponent(currentSourceControllerId)),
                abilityId to ComponentContainer.of(ability),
            ),
            stack = listOf(abilityId),
        )

        val result = StackResolver(cardRegistry = CardRegistry()).counterAbility(state, abilityId)

        val event = result.events.filterIsInstance<AbilityCounteredEvent>().single()
        event.sourceId shouldBe sourceId
        event.sourceName shouldBe "Changed Source"
        event.controllerId shouldBe abilityControllerId
    }

    test("legacy serialized countered-ability events retain compatible defaults") {
        val legacy = """{
            "type":"AbilityCounteredEvent",
            "abilityEntityId":"legacy-ability",
            "description":"Legacy ability"
        }""".trimIndent()

        val decoded = json.decodeFromString<GameEvent>(legacy) as AbilityCounteredEvent

        decoded.sourceId shouldBe null
        decoded.sourceName shouldBe null
        decoded.controllerId shouldBe null
    }
})
