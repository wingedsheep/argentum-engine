package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Playful Shove
 * {1}{R}
 * Sorcery
 *
 * Playful Shove deals 1 damage to any target.
 * Draw a card.
 */
val PlayfulShove = card("Playful Shove") {
    manaCost = "{1}{R}"
    typeLine = "Sorcery"
    oracleText = "Playful Shove deals 1 damage to any target.\nDraw a card."

    spell {
        val t = target("any target", Targets.Any)
        effect = Effects.DealDamage(1, t)
            .then(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "145"
        artist = "Zoltan Boros"
        flavorText = "\"The boisterous pat on the back was painful, but not nearly as painful as the joke that inspired it.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07956edf-34c1-4218-9784-ddbca13e380c.jpg?1721426672"

        ruling("2024-07-26", "If the target is illegal as Playful Shove tries to resolve, it won't resolve and none of its effects will happen. You won't draw a card.")
    }
}
