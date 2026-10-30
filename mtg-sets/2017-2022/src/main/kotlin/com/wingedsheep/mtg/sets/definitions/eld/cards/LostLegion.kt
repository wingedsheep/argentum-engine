package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Lost Legion
 * {1}{B}{B}
 * Creature — Spirit Knight
 * 2/3
 *
 * When this creature enters, scry 2.
 */
val LostLegion = card("Lost Legion") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit Knight"
    oracleText = "When this creature enters, scry 2. (Look at the top two cards of your library, then put any number of them on the bottom and the rest on top in any order.)"
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Scry(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Anastasia Ovchinnikova"
        flavorText = "Even death cannot diminish the persistence of the knights of Locthwain."
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d6139d3-5397-4403-9c1e-312c11a7542b.jpg?1783932635"
    }
}
