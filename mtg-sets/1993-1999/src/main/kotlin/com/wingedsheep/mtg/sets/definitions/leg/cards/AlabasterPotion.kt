package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Alabaster Potion
 * {X}{W}{W}
 * Instant
 * Choose one —
 * • Target player gains X life.
 * • Prevent the next X damage that would be dealt to any target this turn.
 */
val AlabasterPotion = card("Alabaster Potion") {
    manaCost = "{X}{W}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Target player gains X life.\n" +
        "• Prevent the next X damage that would be dealt to any target this turn."
    spell {
        modal(chooseCount = 1) {
            mode("Target player gains X life") {
                val player = target(Targets.Player)
                effect = Effects.GainLife(DynamicAmounts.xValue(), player)
            }
            mode("Prevent the next X damage that would be dealt to any target this turn") {
                val anyTarget = target(Targets.Any)
                effect = Effects.PreventNextDamage(DynamicAmounts.xValue(), anyTarget)
            }
        }
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "Harold McNeill"
        flavorText = "\"Healing is a matter of time, but it is sometimes also a matter of opportunity.\" —D'Avenant proverb"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/2806c7f6-8fdd-4e65-9c71-f2e8b0cdede2.jpg?1783948088"
    }
}
