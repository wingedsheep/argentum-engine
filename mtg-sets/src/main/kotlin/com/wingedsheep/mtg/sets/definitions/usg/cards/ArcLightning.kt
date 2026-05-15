package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Arc Lightning
 * {2}{R}
 * Sorcery
 * Arc Lightning deals 3 damage divided as you choose among one, two, or three targets.
 */
val ArcLightning = card("Arc Lightning") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Arc Lightning deals 3 damage divided as you choose among one, two, or three targets."

    spell {
        target = AnyTarget(count = 3, minCount = 1)
        effect = DividedDamageEffect(
            totalDamage = 3,
            minTargets = 1,
            maxTargets = 3
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "174"
        artist = "Andrew Goldhawk"
        flavorText = "Rainclouds don't last long in Shiv, but that doesn't stop the lightning."
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c81ade7-0074-4447-ba2c-b16fa0f09ccb.jpg?1562897576"
    }
}
