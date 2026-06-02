package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Starfighter Pilot
 * {1}{W}
 * Creature — Human Pilot
 * 2/2
 *
 * Whenever this creature becomes tapped, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)
 */
val StarfighterPilot = card("Starfighter Pilot") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Pilot"
    oracleText = "Whenever this creature becomes tapped, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.BecomesTapped
        effect = com.wingedsheep.sdk.dsl.LibraryPatterns.surveil(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Nathaniel Himawan"
        flavorText = "\"Brightstar-1 to FlightComm: Scopes are clear. The stars are yours.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e571246-aadc-4d1f-a284-9a529e150fe0.jpg?1752946702"
    }
}
