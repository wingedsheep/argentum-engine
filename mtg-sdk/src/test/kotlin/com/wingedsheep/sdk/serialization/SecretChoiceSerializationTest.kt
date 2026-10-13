package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.SecretChoice
import com.wingedsheep.sdk.scripting.effects.SecretChoiceEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class SecretChoiceSerializationTest : FunSpec({
    test("bounded pair comparisons retain participant selectors and every branch") {
        val effect = Effects.SecretChoice(
            participants = listOf(Player.You, Player.BoundVariable("opponent", "target opponent")),
            choice = SecretChoice.Number(
                minValue = 1,
                maxValue = DynamicAmount.XValue,
                outcomeMinValue = 0,
                highestEffect = Effects.DrawCards(1),
                lowestEffect = Effects.DrawCards(2),
                tiedEffect = Effects.DrawCards(3),
                matchedEffect = Effects.GainLife(4),
                unmatchedEffect = Effects.LoseLife(5),
            ),
            then = Effects.DrawCards(6),
        )
        val json = CardSerialization.json
        json.decodeFromString<Effect>(json.encodeToString<Effect>(effect)) shouldBe effect
    }

    test("optional entity ballots retain input and output collection names") {
        val effect = Effects.SecretChoice(
            choice = SecretChoice.Entity("eligible", optional = true, storeMostChosen = "winners"),
            then = Effects.DrawCards(1),
        )
        val json = CardSerialization.json
        json.decodeFromString<Effect>(json.encodeToString<Effect>(effect)) shouldBe effect
    }

    test("migrated numeric facade preserves the former bid defaults") {
        val effect = Effects.SecretNumbers(highestEffect = Effects.DrawCards(1)) as SecretChoiceEffect
        effect.participants shouldBe listOf(Player.Each)
        effect.choice shouldBe SecretChoice.Number(highestEffect = Effects.DrawCards(1))
        effect.then shouldBe null
    }
})
