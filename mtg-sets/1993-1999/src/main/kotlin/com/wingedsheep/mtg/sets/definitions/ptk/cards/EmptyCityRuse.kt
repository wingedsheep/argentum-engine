package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Empty City Ruse
 * {W}
 * Sorcery
 * Target opponent skips all combat phases of their next turn.
 */
val EmptyCityRuse = card("Empty City Ruse") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Target opponent skips all combat phases of their next turn."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.SkipCombatPhases(opponent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "3"
        artist = "Qu Xin"
        flavorText = "Out of time and options, Kongming was forced to bluff at Xicheng. He tricked an army of 150,000 Wei by leaving the city gates open and calmly playing the zither."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d37c84c-80e5-453d-bd2e-4f77ff864c89.jpg?1783946132"
    }
}
