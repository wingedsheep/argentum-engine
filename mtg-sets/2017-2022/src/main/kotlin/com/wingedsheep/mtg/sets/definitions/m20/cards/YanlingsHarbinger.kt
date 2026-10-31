package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Yanling's Harbinger — Core Set 2020 #289 (planeswalker deck)
 * {3}{U}{U} · Creature — Bird · 2/4
 *
 * Flying
 * When this creature enters, you may search your library and/or graveyard for a card named
 * Mu Yanling, Celestial Wind, reveal it, and put it into your hand. If you search your library
 * this way, shuffle.
 *
 * Chandra's Flame Wave's [Patterns.Library.searchLibraryAndOrGraveyard], made optional for the
 * "you may".
 */
val YanlingsHarbinger = card("Yanling's Harbinger") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird"
    power = 2
    toughness = 4
    oracleText = "Flying\nWhen this creature enters, you may search your library and/or graveyard for a " +
        "card named Mu Yanling, Celestial Wind, reveal it, and put it into your hand. If you search " +
        "your library this way, shuffle."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Mu Yanling, Celestial Wind"),
            destination = SearchDestination.HAND,
            reveal = true,
            optional = true
        )
        description = "When this creature enters, you may search your library and/or graveyard for a " +
            "card named Mu Yanling, Celestial Wind, reveal it, and put it into your hand. If you search " +
            "your library this way, shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "289"
        artist = "Zezhou Chen"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93aca389-6830-4297-97bc-e3a2a1a1d41a.jpg?1783932920"
    }
}
