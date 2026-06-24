package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetPlayer


/**
 * Healing Salve
 * {W}
 * Instant
 * Choose one —
 * • Target player gains 3 life.
 * • Prevent the next 3 damage that would be dealt to any target this turn.
 */
val HealingSalve = card("Healing Salve") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Target player gains 3 life.\n" +
        "• Prevent the next 3 damage that would be dealt to any target this turn."
    spell {
        modal(chooseCount = 1) {
            mode("Target player gains 3 life") {
                val player = target("target player", TargetPlayer())
                effect = Effects.GainLife(3, player)
            }
            mode("Prevent the next 3 damage that would be dealt to any target this turn") {
                val anyTarget = target("any target", Targets.Any)
                effect = Effects.PreventNextDamage(3, anyTarget)
            }
        }
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e28de37e-84d5-4dc7-b36c-e14da5924729.jpg?1559591521"
    }
}
