package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mind Sculpt - {1}{U}
 * Sorcery
 *
 * Target opponent mills seven cards.
 */
val MindSculpt = card("Mind Sculpt") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Target opponent mills seven cards."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Patterns.Library.mill(7, opponent)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "61"
        artist = "Michael C. Hayes"
        flavorText = "\"Your mind was a curious mix of madness and genius. I just took away the genius.\"\n—Jace Beleren"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/5870d18e-0303-4722-b7f2-a751f8e372be.jpg?1783940504"
        ruling(
            "2012-07-01",
            "You can cast Mind Sculpt no matter how many cards are in the opponent's library. If there " +
                "are fewer than seven, the opponent will put all of them into their graveyard.",
        )
    }
}
