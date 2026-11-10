package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Behold the Beyond
 * {5}{B}{B}
 * Sorcery
 *
 * Discard your hand. Search your library for three cards, put them into your hand, then shuffle.
 *
 * The discard runs first, so the three tutored cards are never at risk of being discarded.
 */
val BeholdTheBeyond = card("Behold the Beyond") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Discard your hand. Search your library for three cards, put them into your hand, then shuffle."

    spell {
        effect = Patterns.Hand.discardHand() then
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Any,
                count = 3,
                destination = SearchDestination.HAND
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "101"
        artist = "Noah Bradley"
        flavorText = "There is power in the abyss, in blackness more profound than the darkest dreams."
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd22350b-d48c-4ae6-b95f-63336fabe0e3.jpg?1783937781"
    }
}
