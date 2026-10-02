package com.wingedsheep.engine.state

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockersThisCombatComponent
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class BlockingRelationshipsSerializationTest : FunSpec({
    val json = Json { serializersModule = engineSerializersModule; encodeDefaults = true; allowStructuredMapKeys = true }
    val blocker = EntityId("blocker"); val attacker = EntityId("attacker")
    test("created blocks preserve edge and creature transitions") {
        val event: GameEvent = BlocksCreatedEvent(mapOf(blocker to listOf(attacker)), emptySet(), setOf(attacker), mapOf(blocker to 1), mapOf(blocker to 2))
        json.decodeFromString<GameEvent>(json.encodeToString(event)) shouldBe event
    }
    test("combat removal notification roundtrips") {
        val event: GameEvent = RemovedFromCombatEvent(blocker)
        json.decodeFromString<GameEvent>(json.encodeToString(event)) shouldBe event
    }
    test("departed blocker identities survive saving the game") {
        val state = GameState(entities = mapOf(attacker to ComponentContainer.of(BlockersThisCombatComponent(setOf(ObjectRef(blocker, 3))))))
        json.decodeFromString<GameState>(json.encodeToString(state)) shouldBe state
    }
    test("attacker defending seat survives saving the game") {
        val defendingPlayer = EntityId("defending-player")
        val state = GameState(entities = mapOf(attacker to ComponentContainer.of(
            AttackingComponent(EntityId("planeswalker"), attackTargetRemoved = true, defendingPlayerId = defendingPlayer)
        )))
        json.decodeFromString<GameState>(json.encodeToString(state)) shouldBe state
    }
    test("legacy attacker without defending seat still decodes") {
        val legacy = """{"defenderId":"defender","bandId":null,"attackTargetRemoved":false}"""
        json.decodeFromString<AttackingComponent>(legacy) shouldBe AttackingComponent(EntityId("defender"))
    }
})
