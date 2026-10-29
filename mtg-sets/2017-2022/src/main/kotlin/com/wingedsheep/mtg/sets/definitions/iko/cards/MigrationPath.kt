package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Migration Path
 * {3}{G}
 * Sorcery
 * Search your library for up to two basic land cards, put them onto the battlefield tapped,
 * then shuffle.
 * Cycling {2} ({2}, Discard this card: Draw a card.)
 */
val MigrationPath = card("Migration Path") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for up to two basic land cards, put them onto the battlefield tapped, " +
        "then shuffle.\nCycling {2} ({2}, Discard this card: Draw a card.)"

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = 2,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "164"
        artist = "Grzegorz Rutkowski"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d345ca63-16a3-4988-93f5-d0e00e8d6ab0.jpg?1783931035"
    }
}
