package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Organ Hoarder
 * {3}{U}
 * Creature — Zombie
 * 3/2
 * When this creature enters, look at the top three cards of your library, then put one of them
 * into your hand and the rest into your graveyard.
 */
val OrganHoarder = card("Organ Hoarder") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, look at the top three cards of your library, then put one of " +
        "them into your hand and the rest into your graveyard."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopAndKeep(count = 3, keepCount = 1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Nicholas Gregory"
        flavorText = "The stitcher died years ago, but her dutiful assistant still keeps the laboratory well stocked."
        imageUri = "https://cards.scryfall.io/normal/front/9/7/9716d384-88d1-415e-882f-7fed9be1adc6.jpg"
    }
}
