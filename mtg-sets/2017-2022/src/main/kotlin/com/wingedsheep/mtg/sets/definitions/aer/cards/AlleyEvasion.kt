package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Alley Evasion
 * {W}
 * Instant
 * Choose one —
 * • Target creature you control gets +1/+2 until end of turn.
 * • Return target creature you control to its owner's hand.
 */
val AlleyEvasion = card("Alley Evasion") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Target creature you control gets +1/+2 until end of turn.\n" +
        "• Return target creature you control to its owner's hand."

    spell {
        modal(chooseCount = 1) {
            mode("Target creature you control gets +1/+2 until end of turn") {
                val t = target(TargetFilter.CreatureYouControl)
                effect = Effects.ModifyStats(1, 2, t)
            }
            mode("Return target creature you control to its owner's hand") {
                val t = target(TargetFilter.CreatureYouControl)
                effect = Effects.ReturnToHand(t)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "6"
        artist = "Scott Murphy"
        flavorText = "\"Sure, you know the law. But I know the streets.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc10173d-25ff-4734-b8f2-84f94fe52b17.jpg?1783936782"
    }
}
