package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Prophet of the Peak
 * {6}
 * Artifact Creature — Cat
 * 5/5
 *
 * When this creature enters, scry 2. (Look at the top two cards of your library, then put any
 * number of them on the bottom and the rest on top in any order.)
 */
val ProphetOfThePeak = card("Prophet of the Peak") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Cat"
    oracleText = "When this creature enters, scry 2. (Look at the top two cards of your library, then put any number of them on the bottom and the rest on top in any order.)"
    power = 5
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.scry(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "227"
        artist = "Titus Lunter"
        flavorText = "It frequently predicts imminent death by devouring."
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58131bf2-2a9f-4b81-9eeb-810372f3896c.jpg"
    }
}
