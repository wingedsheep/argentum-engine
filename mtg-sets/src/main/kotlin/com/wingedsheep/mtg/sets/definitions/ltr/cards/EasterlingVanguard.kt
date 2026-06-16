package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Easterling Vanguard
 * {1}{B}
 * Creature — Human Warrior
 * 2/1
 *
 * When this creature dies, amass Orcs 1.
 */
val EasterlingVanguard = card("Easterling Vanguard") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 1
    oracleText = "When this creature dies, amass Orcs 1. (Put a +1/+1 counter on an Army you control. " +
        "It's also an Orc. If you don't control an Army, create a 0/0 black Orc Army creature token first.)"

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.Amass(1, "Orc")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "83"
        flavorText = "An army of Easterlings charged forth, while from the hills poured Orcs innumerable."
        artist = "Javier Charro"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e860dd40-07c5-47c3-92a8-1ee95a953c2f.jpg?1686968439"
    }
}
