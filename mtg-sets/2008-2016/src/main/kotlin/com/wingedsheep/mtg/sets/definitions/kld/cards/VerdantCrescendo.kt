package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Verdant Crescendo — Kaladesh #273 (Planeswalker Deck)
 * {3}{G} · Sorcery
 *
 * Search your library for a basic land card and put it onto the battlefield tapped. Search your
 * library and graveyard for a card named Nissa, Nature's Artisan, reveal it, put it into your
 * hand, then shuffle.
 *
 * Unlike the "and/or" tutors (Liberating Combustion), the second search always covers both zones
 * and is mandatory, so it is a plain [Patterns.Library.searchMultipleZones] over library and
 * graveyard rather than the zone-choice wrapper. The one "then shuffle" closes both searches, so
 * the land search doesn't shuffle on its own.
 */
val VerdantCrescendo = card("Verdant Crescendo") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for a basic land card and put it onto the battlefield tapped. " +
        "Search your library and graveyard for a card named Nissa, Nature's Artisan, reveal it, put it " +
        "into your hand, then shuffle."

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = false
        ) then Patterns.Library.searchMultipleZones(
            zones = listOf(Zone.LIBRARY, Zone.GRAVEYARD),
            filter = GameObjectFilter.Any.named("Nissa, Nature's Artisan"),
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "273"
        inBooster = false
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea890019-f48f-4164-b057-773499ef273f.jpg?1783937135"
    }
}
