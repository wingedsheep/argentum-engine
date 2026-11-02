package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Domri's Nodorog — Ravnica Allegiance #272 (planeswalker deck)
 * {3}{R}{G} · Creature — Beast · 5/2
 *
 * Trample
 * When this creature enters, you may search your library and/or graveyard for a card named
 * Domri, City Smasher, reveal it, and put it into your hand. If you search your library this
 * way, shuffle.
 *
 * Same shape as Goldmane Griffin: [Patterns.Library.searchLibraryAndOrGraveyard], made optional
 * for the "you may".
 */
val DomrisNodorog = card("Domri's Nodorog") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Beast"
    power = 5
    toughness = 2
    oracleText = "Trample\nWhen this creature enters, you may search your library and/or graveyard " +
        "for a card named Domri, City Smasher, reveal it, and put it into your hand. If you search " +
        "your library this way, shuffle."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Domri, City Smasher"),
            destination = SearchDestination.HAND,
            reveal = true,
            optional = true
        )
        description = "When this creature enters, you may search your library and/or graveyard for a " +
            "card named Domri, City Smasher, reveal it, and put it into your hand. If you search " +
            "your library this way, shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "272"
        inBooster = false
        artist = "Svetlin Velinov"
        flavorText = "\"Good girl.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1abe58d8-67d1-4719-8e84-27747dea3506.jpg?1783933607"
    }
}
