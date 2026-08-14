package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Walking Sarcophagus
 * {2}
 * Artifact Creature — Zombie Cat
 * 2/1
 * Start your engines! (If you have no speed, it starts at 1. It increases once on each of your turns when an opponent loses life. Max speed is 4.)
 * Max speed — This creature gets +1/+2.
 */
val WalkingSarcophagus = card("Walking Sarcophagus") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Zombie Cat"
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "Max speed — This creature gets +1/+2."
    power = 2
    toughness = 1
    startYourEngines()
    maxSpeed {
        staticAbility { ability = ModifyStats(1, 2, GroupFilter.source()) }
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "246"
        artist = "Julia Metzger"
        flavorText = "The warrior rose in a very different world from the one he'd left behind."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89ccbd73-9414-48a3-bdcf-e838fcffc08f.jpg"
    }
}
