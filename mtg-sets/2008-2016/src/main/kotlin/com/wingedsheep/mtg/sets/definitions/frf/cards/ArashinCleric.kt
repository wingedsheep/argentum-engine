package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Arashin Cleric
 * {1}{W}
 * Creature — Human Cleric
 * 1/3
 * When this creature enters, you gain 3 life.
 */
val ArashinCleric = card("Arashin Cleric") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    oracleText = "When this creature enters, you gain 3 life."
    power = 1
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(3)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "Chris Rahn"
        flavorText = "\"We'll have time to put out the fires when those trapped within the ruins are safe.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10aeac19-6892-448e-9e5f-302051a089fc.jpg"
    }
}
