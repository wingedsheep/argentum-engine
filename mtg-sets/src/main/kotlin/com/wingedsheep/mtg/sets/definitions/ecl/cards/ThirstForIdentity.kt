package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Thirst for Identity
 * {2}{U}
 * Instant
 *
 * Draw three cards. Then discard two cards unless you discard a creature card.
 *
 * The discard instruction is one selection: a single creature card satisfies it, otherwise two
 * cards must be selected.
 */
val ThirstForIdentity = card("Thirst for Identity") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw three cards. Then discard two cards unless you discard a creature card."

    spell {
        effect = Effects.DrawCards(3)
            .then(Effects.DiscardUnlessMatching(2, GameObjectFilter.Creature))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Danny Schwartz"
        flavorText = "Within each rimekin rages an ongoing struggle for self-discovery and purpose."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3949f8c-d1c5-45c2-80ed-a57f4f9af86e.jpg?1767957051"
    }
}
