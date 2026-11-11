package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mist Intruder
 * {1}{U}
 * Creature — Eldrazi Drone
 * 1/2
 * Devoid (This card has no color.)
 * Flying
 * Ingest (Whenever this creature deals combat damage to a player, that player exiles the top card of their library.)
 *
 * Ingest is engine-live: declaring the keyword is the whole ability (see `Ingest`).
 */
val MistIntruder = card("Mist Intruder") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Drone"
    power = 1
    toughness = 2
    oracleText = "Devoid (This card has no color.)\n" +
        "Flying\n" +
        "Ingest (Whenever this creature deals combat damage to a player, that player exiles the top card of their library.)"

    keywords(Keyword.DEVOID, Keyword.FLYING, Keyword.INGEST)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "61"
        artist = "Jason Rainville"
        flavorText = "The wastes spread as quickly as the wind."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8c2e662-d8c3-44d4-b666-fe0d4a046271.jpg?1783938212"
        ruling("2015-08-25", "The card exiled by the ingest ability is exiled face up.")
        ruling("2015-08-25", "If the player has no cards in their library when the ingest ability resolves, nothing happens. That player won't lose the game (until they have to draw a card from an empty library).")
    }
}
