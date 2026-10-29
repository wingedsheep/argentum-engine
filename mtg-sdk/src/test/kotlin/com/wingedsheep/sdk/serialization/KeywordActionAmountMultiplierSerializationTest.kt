package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyKeywordActionAmount
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/** The `multiplier` axis of [ModifyKeywordActionAmount] (Bruvac the Grandiloquent). */
class KeywordActionAmountMultiplierSerializationTest : FunSpec({
    val bruvac = ModifyKeywordActionAmount(EventPattern.MillEvent(Player.EachOpponent), multiplier = 2)

    test("a multiplier round-trips") {
        val encoded = CardSerialization.json.encodeToString<ReplacementEffect>(bruvac)
        CardSerialization.json.decodeFromString<ReplacementEffect>(encoded) shouldBe bruvac
    }

    test("JSON written before the axis existed reads as multiplier 1") {
        val additive = ModifyKeywordActionAmount(EventPattern.MillEvent(Player.EachOpponent), modifier = 4)
        val legacy = CardSerialization.json.encodeToString<ReplacementEffect>(additive)
            .replace(Regex(",?\"multiplier\":1"), "")
        CardSerialization.json.decodeFromString<ReplacementEffect>(legacy) shouldBe additive
    }

    test("the description reads like the printed text") {
        bruvac.description shouldBe
            "If an opponent would mill one or more cards, they mill twice that many cards instead"
    }

    test("a multiplier below 1, or on a spell copy, is rejected") {
        shouldThrow<IllegalArgumentException> {
            ModifyKeywordActionAmount(EventPattern.MillEvent(), multiplier = 0)
        }
        shouldThrow<IllegalArgumentException> {
            ModifyKeywordActionAmount(EventPattern.SpellCopiedEvent(), multiplier = 2)
        }
    }
})
