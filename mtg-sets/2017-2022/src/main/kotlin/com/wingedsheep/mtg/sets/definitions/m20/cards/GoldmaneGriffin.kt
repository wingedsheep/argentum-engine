package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Goldmane Griffin — Core Set 2020 #283 (planeswalker deck)
 * {3}{W}{W} · Creature — Griffin · 3/2
 *
 * Flying, vigilance
 * When this creature enters, you may search your library and/or graveyard for a card named
 * Ajani, Inspiring Leader, reveal it, and put it into your hand. If you search your library
 * this way, shuffle.
 *
 * Same shape as Yanling's Harbinger: [Patterns.Library.searchLibraryAndOrGraveyard], made
 * optional for the "you may".
 */
val GoldmaneGriffin = card("Goldmane Griffin") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Griffin"
    power = 3
    toughness = 2
    oracleText = "Flying, vigilance\nWhen this creature enters, you may search your library and/or graveyard " +
        "for a card named Ajani, Inspiring Leader, reveal it, and put it into your hand. If you search " +
        "your library this way, shuffle."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Ajani, Inspiring Leader"),
            destination = SearchDestination.HAND,
            reveal = true,
            optional = true
        )
        description = "When this creature enters, you may search your library and/or graveyard for a " +
            "card named Ajani, Inspiring Leader, reveal it, and put it into your hand. If you search " +
            "your library this way, shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "283"
        artist = "Paul Scott Canavan"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67edcb9b-f56b-4a34-b4bd-d22ee929041a.jpg?1783932923"
    }
}
