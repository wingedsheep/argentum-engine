package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Graven Lore
 * {3}{U}{U}
 * Snow Instant
 * Scry X, where X is the amount of {S} spent to cast this spell, then draw three cards.
 *
 * "{S} spent" is any mana from a snow source spent on the cost (CR 107.4h), read off the spell's
 * recorded payment by `SnowManaSpent` at resolution. With no snow mana spent (or on a copy) X is 0,
 * which is no scry at all, and the spell just draws three.
 */
val GravenLore = card("Graven Lore") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Snow Instant"
    oracleText = "Scry X, where X is the amount of {S} spent to cast this spell, then draw three cards. " +
        "({S} is mana from a snow source.)"

    spell {
        effect = Effects.Scry(DynamicAmounts.snowManaSpent()) then Effects.DrawCards(3)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4386e5c-32b1-4096-a93f-fedd1a4801fc.jpg?1783928262"
        ruling("2021-02-05", "If you cast Graven Lore without spending snow mana, you'll just draw three cards.")
        ruling("2021-02-05", "If Graven Lore is copied, no mana was spent to cast the copy, so you'll just draw three cards.")
    }
}
