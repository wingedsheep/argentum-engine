package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.UntapLimitPerStep
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class ConditionalUntapLimitSerializationTest : FunSpec({
    test("conditional filtered untap caps round trip polymorphically") {
        val ability: StaticAbility = ConditionalStaticAbility(
            UntapLimitPerStep(GameObjectFilter.Land.withSubtype("Forest").youControl(), 1),
            Conditions.SourceIsUntapped)
        CardSerialization.json.decodeFromString<StaticAbility>(
            CardSerialization.json.encodeToString(ability)) shouldBe ability
    }

    test("player-scoped untap caps round trip and default to every player") {
        val ability: StaticAbility = UntapLimitPerStep(GameObjectFilter.Permanent, 2, players = Player.EachOpponent)
        CardSerialization.json.decodeFromString<StaticAbility>(
            CardSerialization.json.encodeToString(ability)) shouldBe ability
        CardSerialization.json.decodeFromString<StaticAbility>(
            CardSerialization.json.encodeToString<StaticAbility>(UntapLimitPerStep(GameObjectFilter.Land, 1))
        ) shouldBe UntapLimitPerStep(GameObjectFilter.Land, 1, players = Player.Each)
    }
})
