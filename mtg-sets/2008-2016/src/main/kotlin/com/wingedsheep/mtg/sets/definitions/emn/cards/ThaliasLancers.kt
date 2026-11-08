package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Thalia's Lancers — Eldritch Moon #47
 * {3}{W}{W} · Creature — Human Knight · 4/4
 *
 * First strike
 * When this creature enters, you may search your library for a legendary card, reveal it, put it
 * into your hand, then shuffle.
 *
 * Same optional ETB tutor shape as Goblin Matron (P02), with Captain Sisay's (INV) legendary-card
 * filter. A "legendary card" is any card with the legendary supertype, so the filter is
 * `Any.legendary()` (legendary sorceries and instants count). Declining the "may" skips the shuffle
 * too, which is why the consent gate wraps the whole search.
 */
val ThaliasLancers = card("Thalia's Lancers") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 4
    toughness = 4
    oracleText = "First strike\nWhen this creature enters, you may search your library for a legendary card, " +
        "reveal it, put it into your hand, then shuffle."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Any.legendary(),
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true,
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "47"
        artist = "David Palumbo"
        flavorText = "\"Let the points of our lances lead the way.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/9/9930d08d-5965-4c16-a84d-43c8ccf2ad0b.jpg?1783937507"
    }
}
