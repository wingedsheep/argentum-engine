package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Magic Missile
 * {1}{R}{R}
 * Sorcery
 * This spell can't be countered.
 * Magic Missile deals 3 damage divided as you choose among one, two, or three targets.
 */
val MagicMissile = card("Magic Missile") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "This spell can't be countered.\n" +
        "Magic Missile deals 3 damage divided as you choose among one, two, or three targets."

    cantBeCountered = true

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
        collectorNumber = "154"
        artist = "PINDURSKI"
        flavorText = "A tried-and-true favorite of many wizards, whatever their school."
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c814ac58-8826-48cd-8209-4af6803866e3.jpg?1783926476"
    }
}
