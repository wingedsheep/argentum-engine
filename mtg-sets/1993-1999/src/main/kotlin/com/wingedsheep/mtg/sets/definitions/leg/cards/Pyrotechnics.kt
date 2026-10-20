package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Pyrotechnics
 * {4}{R}
 * Sorcery
 * Pyrotechnics deals 4 damage divided as you choose among any number of targets.
 *
 * Each target must be assigned at least 1 damage, so "any number" caps at four targets.
 */
val Pyrotechnics = card("Pyrotechnics") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Pyrotechnics deals 4 damage divided as you choose among any number of targets."

    spell {
        target = AnyTarget(count = 4, minCount = 1)
        effect = Effects.DividedDamage(
            total = 4,
            minTargets = 1,
            maxTargets = 4
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "161"
        artist = "Anson Maddocks"
        flavorText = "\"Hi! ni! ya! Behold the man of flint, that's me!/ Four lightnings zigzag from me, strike and return.\" —Navajo war chant"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/2646284b-a94d-4c99-98d4-7becbb473e2b.jpg?1783948053"
        ruling("2014-11-24", "You choose how the damage is divided as you cast the spell, not as it resolves. Each target must be assigned at least 1 damage.")
        ruling("2014-11-24", "If some but not all of Pyrotechnics's targets become illegal, you can't change the division of damage. Damage that would have been dealt to illegal targets simply isn't dealt.")
    }
}
