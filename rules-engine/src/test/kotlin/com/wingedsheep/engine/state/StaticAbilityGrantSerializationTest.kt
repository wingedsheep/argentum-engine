package com.wingedsheep.engine.state

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.StaticAbilityGrantedEvent
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.event.GrantedStaticAbility
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantBeAttackedBy
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class StaticAbilityGrantSerializationTest : FunSpec({
    val json = Json { serializersModule = engineSerializersModule; encodeDefaults = true; allowStructuredMapKeys = true }
    val player = EntityId("protected-player")
    test("static grant notifications survive the event protocol") {
        val event: GameEvent = StaticAbilityGrantedEvent(player)
        json.decodeFromString<GameEvent>(json.encodeToString(event)) shouldBe event
    }
    test("saved player restriction retains source controller filter and duration") {
        val state = GameState(grantedStaticAbilities = listOf(GrantedStaticAbility(
            player, CantBeAttackedBy(GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING)),
            Duration.UntilYourNextTurn, EntityId("departed-source"), player)))
        json.decodeFromString<GameState>(json.encodeToString(state)) shouldBe state
    }
})
