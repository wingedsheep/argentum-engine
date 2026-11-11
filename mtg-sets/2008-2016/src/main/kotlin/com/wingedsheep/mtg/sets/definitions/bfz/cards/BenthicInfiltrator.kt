package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Benthic Infiltrator
 * {2}{U}
 * Creature — Eldrazi Drone
 * 1/4
 * Devoid (This card has no color.)
 * Ingest (Whenever this creature deals combat damage to a player, that player exiles the top card of their library.)
 * This creature can't be blocked.
 *
 * Ingest is engine-live: declaring the keyword is the whole ability (see `Ingest`).
 */
val BenthicInfiltrator = card("Benthic Infiltrator") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Drone"
    power = 1
    toughness = 4
    oracleText = "Devoid (This card has no color.)\n" +
        "Ingest (Whenever this creature deals combat damage to a player, that player exiles the top card of their library.)\n" +
        "This creature can't be blocked."

    keywords(Keyword.DEVOID, Keyword.INGEST)
    flags(AbilityFlag.CANT_BE_BLOCKED)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f380ae58-08cc-42df-ac74-3c6890a62463.jpg?1783938214"
        ruling("2015-08-25", "The card exiled by the ingest ability is exiled face up.")
        ruling("2015-08-25", "If the player has no cards in their library when the ingest ability resolves, nothing happens. That player won't lose the game (until they have to draw a card from an empty library).")
    }
}
