package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Sorin's Guide — Core Set 2020 #292 (planeswalker deck)
 * {3}{B}{B} · Creature — Vampire · 4/2
 *
 * When this creature enters, you may search your library and/or graveyard for a card named
 * Sorin, Vampire Lord, reveal it, and put it into your hand. If you search your library this
 * way, shuffle.
 *
 * Same shape as Yanling's Harbinger: [Patterns.Library.searchLibraryAndOrGraveyard], optional.
 */
val SorinsGuide = card("Sorin's Guide") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 4
    toughness = 2
    oracleText = "When this creature enters, you may search your library and/or graveyard for a card " +
        "named Sorin, Vampire Lord, reveal it, and put it into your hand. If you search your library " +
        "this way, shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Sorin, Vampire Lord"),
            destination = SearchDestination.HAND,
            reveal = true,
            optional = true
        )
        description = "When this creature enters, you may search your library and/or graveyard for a " +
            "card named Sorin, Vampire Lord, reveal it, and put it into your hand. If you search your " +
            "library this way, shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "292"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/416b000d-77d0-4c83-bbbd-d1107f746dde.jpg?1783932920"
    }
}
