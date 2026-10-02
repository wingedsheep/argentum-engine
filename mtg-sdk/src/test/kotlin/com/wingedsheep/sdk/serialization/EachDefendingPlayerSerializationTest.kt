package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class EachDefendingPlayerSerializationTest : FunSpec({
    test("defending player scope round-trips in a player loop and effect target") {
        val effect: Effect = Effects.ForEachPlayer(
            Player.EachDefendingPlayer,
            listOf(Effects.DrawCards(1)),
        ).then(Effects.GainLife(1, EffectTarget.PlayerRef(Player.EachDefendingPlayer)))
        val encoded = CardSerialization.json.encodeToString<Effect>(effect)
        CardSerialization.json.decodeFromString<Effect>(encoded) shouldBe effect
        Player.EachDefendingPlayer.possessive shouldBe "each defending player's"
    }
})
