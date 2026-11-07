package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Byway Courier
 * {2}{G}
 * Creature — Human Scout
 * 3/2
 * When this creature dies, investigate.
 */
val BywayCourier = card("Byway Courier") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Scout"
    oracleText = "When this creature dies, investigate. (Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Investigate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "196"
        artist = "Zoltan Boros"
        flavorText = "A good courier travels fast and asks no questions."
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f243a51-bbd3-4028-a469-469ab5591aae.jpg?1783937737"
    }
}
