package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Precognitive Perception
 * {3}{U}{U}
 * Instant
 * Draw three cards.
 * Addendum — If you cast this spell during your main phase, instead scry 3, then draw three cards.
 *
 * Addendum is checked at resolution: [Conditions.WasCast] excludes copies (never cast), and
 * [Conditions.IsYourMainPhase] reads the cast-time phase, since a phase only ends with an empty stack.
 */
val PrecognitivePerception = card("Precognitive Perception") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw three cards.\n" +
        "Addendum — If you cast this spell during your main phase, instead scry 3, then draw three cards."

    spell {
        effect = Effects.If(
            condition = Conditions.All(Conditions.WasCast, Conditions.IsYourMainPhase),
            then = Effects.Scry(3) then Effects.DrawCards(3),
            otherwise = Effects.DrawCards(3),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "45"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/724fd1ea-05ad-497c-9989-825ada48e684.jpg?1783933707"
    }
}
