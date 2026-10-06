package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Flames of the Firebrand
 * {2}{R}
 * Sorcery
 * Flames of the Firebrand deals 3 damage divided as you choose among one, two, or three targets.
 */
val FlamesOfTheFirebrand = card("Flames of the Firebrand") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Flames of the Firebrand deals 3 damage divided as you choose among one, two, or three targets."

    spell {
        target = AnyTarget(count = 3, minCount = 1)
        effect = Effects.DividedDamage(
            total = 3,
            minTargets = 1,
            maxTargets = 3
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Steve Argyle"
        flavorText = "\"You're in luck. I brought enough to share.\"\n—Chandra Nalaar"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/aca215b1-7b98-49ce-afae-eeb61058125a.jpg?1783940483"
    }
}
