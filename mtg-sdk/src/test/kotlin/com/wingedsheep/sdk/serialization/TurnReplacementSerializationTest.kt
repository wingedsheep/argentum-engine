package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class TurnReplacementSerializationTest : FunSpec({
    test("turn replacement preserves composed follow-up player scope and condition") {
        val effect: ReplacementEffect = OptionalSkipTurnWith(
            Effects.Untap(EffectTarget.Self).then(Effects.DrawCards(1)),
            appliesTo = EventPattern.TurnBeginEvent(Player.EachOpponent),
            restrictions = listOf(Conditions.SourceIsTapped))
        val encoded = CardSerialization.json.encodeToString<ReplacementEffect>(effect)
        CardSerialization.json.decodeFromString<ReplacementEffect>(encoded) shouldBe effect
    }
})
