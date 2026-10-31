package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Kiora's Dambreaker
 * {5}{U}
 * Creature — Leviathan
 * 5/6
 * When this creature enters, proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)
 */
val KiorasDambreaker = card("Kiora's Dambreaker") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Leviathan"
    power = 5
    toughness = 6
    oracleText = "When this creature enters, proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "58"
        artist = "Mathias Kollros"
        flavorText = "\"When your enemy flees, send the sea to pursue them.\"\n—Kiora"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acd61fb0-a82b-4f46-8043-c6b89764720b.jpg?1783933460"
    }
}
