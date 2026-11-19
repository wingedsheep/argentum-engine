package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Plea for Guidance
 * {5}{W}
 * Sorcery
 *
 * Search your library for up to two enchantment cards, reveal them, put them into your hand, then
 * shuffle.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val PleaForGuidance = card("Plea for Guidance") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Search your library for up to two enchantment cards, reveal them, put them into your " +
        "hand, then shuffle."

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Enchantment,
            count = 2,
            destination = SearchDestination.HAND,
            reveal = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Terese Nielsen"
        flavorText = "On Theros, the dove is a symbol not of peace but of discovery. Quick wings herald " +
            "the arrival of a revelation."
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d1d6d76-f399-475a-b5cd-544bd7c6d967.jpg?1783939576"
    }
}
