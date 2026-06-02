package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Space-Time Anomaly
 * {2}{W}{U}
 * Sorcery
 * Target player mills cards equal to your life total.
 */
val SpaceTimeAnomaly = card("Space-Time Anomaly") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Sorcery"
    oracleText = "Target player mills cards equal to your life total."

    spell {
        val player = target("player", Targets.Player)
        effect = LibraryPatterns.mill(DynamicAmount.YourLifeTotal, player)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "229"
        artist = "Loïc Canavaggia"
        flavorText = "The howling winds could hardly be heard over the maddening thoughts of the godcore."
        imageUri = "https://cards.scryfall.io/normal/front/e/d/edb8dc2a-ddce-48fa-b57e-0e57c87c6671.jpg?1752947494"
    }
}
