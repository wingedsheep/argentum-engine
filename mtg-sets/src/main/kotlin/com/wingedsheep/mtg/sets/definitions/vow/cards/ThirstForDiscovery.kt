package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Thirst for Discovery
 * {2}{U}
 * Instant
 *
 * Draw three cards. Then discard two cards unless you discard a basic land card.
 *
 * The discard instruction is one selection: a single basic land card satisfies it, otherwise two
 * cards must be selected.
 */
val ThirstForDiscovery = card("Thirst for Discovery") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw three cards. Then discard two cards unless you discard a basic land card."

    spell {
        effect = Effects.DrawCards(3)
            .then(Effects.DiscardUnlessMatching(2, GameObjectFilter.BasicLand))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "85"
        artist = "Dominik Mayer"
        flavorText = "\"This is your only warning, alchemist. The secrets of the sea are not yours to behold. Lord Krothuss will not be so merciful next time.\"\n—Runo Stromkirk"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1ea179e9-9c0d-46c1-9ee8-60be68e1f79c.jpg?1643588791"
    }
}
