package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Planar Ally
 * {3}{W}{W}
 * Creature — Angel
 * 3/3
 * Flying
 * Whenever this creature attacks, venture into the dungeon. (Enter the first room or advance to the next room.)
 */
val PlanarAlly = card("Planar Ally") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever this creature attacks, venture into the dungeon. (Enter the first room or advance to the next room.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.VentureIntoTheDungeon()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9e7457a-8ab4-4f3c-a2d8-2da4d81b7470.jpg?1783926525"
    }
}
