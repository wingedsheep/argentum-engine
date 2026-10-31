package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bloom Hulk
 * {3}{G}
 * Creature — Plant Elemental
 * 4/4
 * When this creature enters, proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)
 */
val BloomHulk = card("Bloom Hulk") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Elemental"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "154"
        artist = "Even Amundsen"
        flavorText = "\"Tread upon these flower beds at your own peril.\"\n—Emmara"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd1c9129-254d-4d1d-9d95-112292bd6bcc.jpg?1783933416"
    }
}
