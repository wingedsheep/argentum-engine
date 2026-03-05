package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyCounterPlacement

/**
 * Hardened Scales
 * {G}
 * Enchantment
 * If one or more +1/+1 counters would be put on a creature you control,
 * that many plus one +1/+1 counters are put on it instead.
 */
val HardenedScales = card("Hardened Scales") {
    manaCost = "{G}"
    typeLine = "Enchantment"
    oracleText = "If one or more +1/+1 counters would be put on a creature you control, that many plus one +1/+1 counters are put on it instead."

    replacementEffect(ModifyCounterPlacement(modifier = 1))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "133"
        artist = "Mark Winters"
        flavorText = "\"Naga shed their scales. We wear ours with pride.\"\n—Golran, dragonscale captain"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7dcdf1db-bfaf-4160-8003-1fa2e56b00dc.jpg?1562789138"
    }
}
