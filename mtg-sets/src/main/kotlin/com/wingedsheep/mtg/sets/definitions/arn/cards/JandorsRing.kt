package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Jandor's Ring
 * {6}
 * Artifact
 * {2}, {T}, Discard the last card you drew this turn: Draw a card.
 */
val JandorsRing = card("Jandor's Ring") {
    manaCost = "{6}"
    typeLine = "Artifact"
    oracleText = "{2}, {T}, Discard the last card you drew this turn: Draw a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.DiscardLastDrawnThisTurn)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "64"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71504078-a16f-4dc4-9626-0ecc42b1e93b.jpg?1562916016"

        ruling(
            "2004-10-04",
            "If you do not have the card still in your hand, you can't pay the cost."
        )
        ruling(
            "2004-10-04",
            "If you draw more than one card due to a spell or ability, you must discard the last one of those drawn."
        )
    }
}
