package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Martyr for the Cause
 * {1}{W}
 * Creature — Human Soldier
 * 2/2
 * When this creature dies, proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)
 */
val MartyrForTheCause = card("Martyr for the Cause") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "When this creature dies, proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "23"
        artist = "Zoltan Boros"
        flavorText = "\"If I am to die in service, there is no more worthy fight than this.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acfbc8ca-a574-461e-92ea-c6010daa8230.jpg?1783933480"
    }
}
