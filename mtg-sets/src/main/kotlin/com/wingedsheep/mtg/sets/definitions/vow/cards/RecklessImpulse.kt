package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Reckless Impulse
 * {1}{R}
 * Sorcery
 *
 * Exile the top two cards of your library. Until the end of your next turn, you may play those cards.
 */
val RecklessImpulse = card("Reckless Impulse") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Exile the top two cards of your library. Until the end of your next turn, you may " +
        "play those cards."

    spell {
        effect = Patterns.Exile.impulse(2, MayPlayExpiry.UntilEndOfNextTurn)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "174"
        artist = "Mathias Kollros"
        flavorText = "A stitcher looks at their creation and sees the result of years of study and " +
            "hours of toil. A devil sees a new plaything."
        imageUri = "https://cards.scryfall.io/normal/front/6/9/6943c07f-ab0d-4f5a-bbe9-c0a83dc98546.jpg?1782703066"
    }
}
