package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Leave in the Dust
 * {3}{U}
 * Instant
 * Return target nonland permanent to its owner's hand.
 * Draw a card.
 *
 * If the target is illegal on resolution the spell doesn't resolve, so no card is drawn.
 */
val LeaveInTheDust = card("Leave in the Dust") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target nonland permanent to its owner's hand.\nDraw a card."

    spell {
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.ReturnToHand(permanent) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Vincent Proce"
        flavorText = "The daredevil racers of the Derby Crows, long at odds with the Consulate, were quick to join the renegades in open revolt."
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c01bd77-beb0-4a26-858d-022311e550bf.jpg?1783936771"
        ruling(
            "2017-02-09",
            "If the target permanent becomes an illegal target, Leave in the Dust doesn’t resolve and none of its effects happen. You won’t draw a card.",
        )
    }
}
