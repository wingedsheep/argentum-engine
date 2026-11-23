package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Firemind's Foresight
 * {5}{U}{R}
 * Instant
 *
 * Search your library for an instant card with mana value 3, reveal it, and put it into your
 * hand. Then repeat this process for instant cards with mana values 2 and 1. Then shuffle.
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 *
 * Three independent up-to-one searches (each may find nothing), each revealing its card into
 * hand, with a single shuffle after the last — [com.wingedsheep.mtg.sets.definitions.khm.cards.AxgardArmory]'s
 * sequential-search shape.
 */
val FiremindsForesight = card("Firemind's Foresight") {
    manaCost = "{5}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "Search your library for an instant card with mana value 3, reveal it, and put it into your hand. " +
        "Then repeat this process for instant cards with mana values 2 and 1. Then shuffle."

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Instant.manaValue(3),
            reveal = true,
            shuffleAfter = false
        ) then Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Instant.manaValue(2),
            reveal = true,
            shuffleAfter = false
        ) then Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Instant.manaValue(1),
            reveal = true,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9cb5cb63-e7ec-4fc3-a389-4d8b5a4b96b9.jpg?1783940340"
    }
}
