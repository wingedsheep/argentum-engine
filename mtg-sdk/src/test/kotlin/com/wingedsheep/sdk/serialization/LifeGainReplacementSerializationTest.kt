package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceLifeGainWith
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class LifeGainReplacementSerializationTest : FunSpec({
    test("life replacement preserves its effect amount scope and restriction") {
        val replacement: ReplacementEffect = ReplaceLifeGainWith(
            Effects.DrawCards(DynamicAmounts.replacementLifeGainAmount()),
            appliesTo = EventPattern.LifeGainEvent(Player.EachOpponent),
            restrictions = listOf(Conditions.SourceIsTapped)
        )
        val encoded = CardSerialization.json.encodeToString<ReplacementEffect>(replacement)
        CardSerialization.json.decodeFromString<ReplacementEffect>(encoded) shouldBe replacement
    }
})
