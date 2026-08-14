package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Swiftwing Assailant — Aetherdrift #32
 * {3}{W} · Creature — Bird Warrior · 3/3
 *
 * Flying
 * Start your engines!
 * Max speed — This creature gets +0/+1 and has vigilance.
 *
 * One printed max-speed ability, two gated statics: the `maxSpeed { }` block conjoins
 * "your speed is 4" onto each, and both are re-read every projection pass rather than latched
 * at any point in time.
 */
val SwiftwingAssailant = card("Swiftwing Assailant") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Warrior"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Start your engines! (If you have no speed, it starts at 1. It increases once on each of " +
        "your turns when an opponent loses life. Max speed is 4.)\n" +
        "Max speed — This creature gets +0/+1 and has vigilance."

    keywords(Keyword.FLYING)
    startYourEngines()

    maxSpeed {
        staticAbility { ability = ModifyStats(0, 1, GroupFilter.source()) }
        keywords(Keyword.VIGILANCE)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Pig Hands"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72db9bb9-d930-40e5-b144-01ebfd377996.jpg?1783907913"
    }
}
