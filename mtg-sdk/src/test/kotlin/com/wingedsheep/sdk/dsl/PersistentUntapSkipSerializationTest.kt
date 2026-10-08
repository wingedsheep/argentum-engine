package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.core.TurnPart
import com.wingedsheep.sdk.scripting.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PersistentUntapSkipSerializationTest : FunSpec({
    test("standing skip round-trips through the static ability contract") {
        val ability: StaticAbility = SkipStepOrPhase(TurnPart.UNTAP_STEP, Player.EachOpponent)
        Json.decodeFromString<StaticAbility>(Json.encodeToString(ability)) shouldBe ability
    }

    test("a standing skip only accepts parts the engine reads") {
        io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
            SkipStepOrPhase(TurnPart.COMBAT_PHASE)
        }
    }
})
