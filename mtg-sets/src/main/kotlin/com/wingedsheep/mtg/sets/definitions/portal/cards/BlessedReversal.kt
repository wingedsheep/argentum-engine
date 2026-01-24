package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.GainLifeEffect

/**
 * Blessed Reversal
 * {1}{W}
 * Instant
 * You gain 3 life for each creature attacking you.
 */
val BlessedReversal = card("Blessed Reversal") {
    manaCost = "{1}{W}"
    typeLine = "Instant"

    spell {
        effect = GainLifeEffect(DynamicAmount.CreaturesAttackingYou(multiplier = 3))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Zina Saunders"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/899ecc19-8106-4e5a-bb25-aaea9684ba0e.jpg"
    }
}
