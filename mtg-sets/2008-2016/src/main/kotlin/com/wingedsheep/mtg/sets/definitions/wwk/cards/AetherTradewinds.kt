package com.wingedsheep.mtg.sets.definitions.wwk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Aether Tradewinds
 * {2}{U}
 * Instant
 * Return target permanent you control and target permanent you don't control to their owners' hands.
 */
val AetherTradewinds = card("Aether Tradewinds") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target permanent you control and target permanent you don't control to their owners' hands."

    spell {
        val yours = target(TargetFilter.PermanentYouControl)
        val theirs = target(TargetFilter.PermanentOpponentControls)
        effect = Effects.ReturnToHand(yours) then Effects.ReturnToHand(theirs)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Kieran Yanner"
        flavorText = "\"The wind smells of misfortune. Check your knots.\"\n—Anitan, Ondu cleric"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99f31a77-a4a9-4783-8885-c44552379ae6.jpg?1783942063"
        ruling(
            "2016-09-20",
            "If one target permanent becomes an illegal target, the other will be returned to its owner's hand. " +
                "If both targets become illegal, Aether Tradewinds won't resolve."
        )
    }
}
