package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sultai Skullkeeper
 * {1}{U}
 * Creature — Snake Shaman
 * 2/1
 * When this creature enters, mill two cards.
 */
val SultaiSkullkeeper = card("Sultai Skullkeeper") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Snake Shaman"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, mill two cards."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Ryan Barger"
        flavorText = "A skullkeeper is the first to arrive after the palace archers strike down intruders, probing their brains for choice bits of knowledge."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21a93355-553c-456b-999b-e9f826a1f572.jpg"
    }
}
