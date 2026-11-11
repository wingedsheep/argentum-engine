package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Culling Drone
 * {1}{B}
 * Creature — Eldrazi Drone
 * 2/2
 * Devoid (This card has no color.)
 * Ingest (Whenever this creature deals combat damage to a player, that player exiles the top card of their library.)
 *
 * Ingest is engine-live: declaring the keyword is the whole ability (see `Ingest`).
 */
val CullingDrone = card("Culling Drone") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Eldrazi Drone"
    power = 2
    toughness = 2
    oracleText = "Devoid (This card has no color.)\n" +
        "Ingest (Whenever this creature deals combat damage to a player, that player exiles the top card of their library.)"

    keywords(Keyword.DEVOID, Keyword.INGEST)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "91"
        artist = "Alejandro Mirabal"
        flavorText = "Silence rules Bala Ged, broken only by the skittering of Eldrazi."
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a623415-46c0-45aa-ab75-1e2d9708013a.jpg?1783938206"
        ruling("2015-08-25", "The card exiled by the ingest ability is exiled face up.")
        ruling("2015-08-25", "If the player has no cards in their library when the ingest ability resolves, nothing happens. That player won't lose the game (until they have to draw a card from an empty library).")
    }
}
