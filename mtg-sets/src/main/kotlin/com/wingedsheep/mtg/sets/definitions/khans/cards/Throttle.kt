package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Throttle
 * {4}{B}
 * Instant
 * Target creature gets -4/-4 until end of turn.
 */
val Throttle = card("Throttle") {
    manaCost = "{4}{B}"
    typeLine = "Instant"
    oracleText = "Target creature gets -4/-4 until end of turn."

    spell {
        effect = Effects.ModifyStats(-4, -4)
        target = Targets.Creature
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Wayne Reynolds"
        flavorText = "\"The best servants are made from those who died without a scratch.\"\n—Sidisi, khan of the Sultai"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6b71c00-3f38-47dc-90bc-6464d346c098.jpg?1562792396"
    }
}
