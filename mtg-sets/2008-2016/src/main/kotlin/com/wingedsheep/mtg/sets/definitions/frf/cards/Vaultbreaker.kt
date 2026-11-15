package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Vaultbreaker
 * {3}{R}
 * Creature — Orc Rogue
 * 4/2
 * Whenever this creature attacks, you may discard a card. If you do, draw a card.
 * Dash {2}{R} (You may cast this spell for its dash cost. If you do, it gains haste, and it's returned from the battlefield to its owner's hand at the beginning of the next end step.)
 */
val Vaultbreaker = card("Vaultbreaker") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Orc Rogue"
    power = 4
    toughness = 2
    oracleText = "Whenever this creature attacks, you may discard a card. If you do, draw a card.\n" +
        "Dash {2}{R} (You may cast this spell for its dash cost. If you do, it gains haste, and it's returned from the battlefield to its owner's hand at the beginning of the next end step.)"

    dash = "{2}{R}"

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.May(
            effect = Effects.IfYouDo(action = Patterns.Hand.discardCards(1), then = Effects.DrawCards(1))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/12891fcf-7118-428a-bdfe-9495a77f8b3d.jpg"
    }
}
