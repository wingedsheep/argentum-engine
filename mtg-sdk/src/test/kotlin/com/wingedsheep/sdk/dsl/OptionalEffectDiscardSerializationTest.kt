package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class OptionalEffectDiscardSerializationTest : FunSpec({
    test("discard destinations round trip scoped players and card filters") {
        val replacement: ReplacementEffect = OptionalEffectDiscardDestination(
            CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            EventPattern.DiscardEvent(Player.EachOpponent, GameObjectFilter.Creature))
        Json.decodeFromString<ReplacementEffect>(Json.encodeToString(replacement)) shouldBe replacement
        replacement.optional shouldBe true
    }
    test("an opponent-caused, revealed discard destination round trips") {
        val replacement: ReplacementEffect = OptionalEffectDiscardDestination(
            CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top),
            EventPattern.DiscardEvent(Player.You, cause = ZoneChangeCause.DiscardedByOpponentEffect),
            reveal = true)
        Json.decodeFromString<ReplacementEffect>(Json.encodeToString(replacement)) shouldBe replacement
        replacement.description shouldBe "You may reveal it and discard into a library (top) instead"
    }
})
