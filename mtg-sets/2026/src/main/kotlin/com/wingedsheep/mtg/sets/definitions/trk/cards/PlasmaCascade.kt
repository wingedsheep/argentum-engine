package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Plasma Cascade
 * {2}{R}
 * Instant
 *
 * Plasma Cascade deals 3 damage to any target. You may discard a card. If you do, draw a card.
 */
val PlasmaCascade = card("Plasma Cascade") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Plasma Cascade deals 3 damage to any target. You may discard a card. If you do, draw a card."

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(3, t) then
            Effects.May(
                effect = Effects.IfYouDo(
                    action = Patterns.Hand.discardCards(1),
                    then = Effects.DrawCards(1),
                ),
                descriptionOverride = "You may discard a card. If you do, draw a card.",
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Narendra Bintara Adi"
        flavorText = "\"Fire in engineering! Response teams, move!\""
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c786a2f-3de4-4b24-a3a0-15b1f2a07324.jpg?1785981557"
    }
}
