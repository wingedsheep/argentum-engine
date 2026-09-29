package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Converter Beast
 * {3}{G}
 * Creature — Phyrexian Beast
 * 0/1
 *
 * When this creature enters, incubate 5.
 */
val ConverterBeast = card("Converter Beast") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Phyrexian Beast"
    oracleText = "When this creature enters, incubate 5. (Create an Incubator token with five +1/+1 counters " +
        "on it and \"{2}: Transform this token.\" It transforms into a 0/0 Phyrexian artifact creature.)"
    power = 0
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Incubate(5)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "180"
        artist = "Uriah Voth"
        flavorText = "Slowly, the distinct screams of the trapped faded into muffled gurgles of glistening oil."
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1bdd3ecb-8c11-4a4c-a503-bc29f79a9dcb.jpg?1783916972"
    }
}
