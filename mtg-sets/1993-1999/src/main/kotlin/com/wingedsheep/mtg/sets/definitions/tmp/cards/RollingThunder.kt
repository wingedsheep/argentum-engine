package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Rolling Thunder — Tempest #198 (canonical printing)
 * {X}{R}{R} · Sorcery
 *
 * Rolling Thunder deals X damage divided as you choose among any number of targets.
 *
 * X is announced first (CR 601.2b), then the targets, then the division (CR 601.2d). Every target
 * must be assigned at least 1 damage, so X also caps how many targets "any number" can be — X = 0
 * means no targets at all (per the ruling). A target that becomes illegal loses its share; the
 * rest keep what they were assigned.
 */
val RollingThunder = card("Rolling Thunder") {
    manaCost = "{X}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Rolling Thunder deals X damage divided as you choose among any number of targets."

    spell {
        target = AnyTarget(
            minCount = 0,
            unlimited = true,
            dynamicMaxCount = DynamicAmounts.xValue(),
        )
        effect = Effects.DividedDamage(total = 0, dynamicTotal = DynamicAmounts.xValue())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "198"
        artist = "Richard Thomas"
        flavorText = "\"Such rage,\" thought Vhati, gazing up at the thunderhead from the *Predator*. " +
            "\"It is Greven's mind manifest.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0bb07402-d526-4938-89a3-9174d5b5a4de.jpg?1783946627"

        ruling("2004-10-04", "If X is 0, you can't choose any targets.")
        ruling(
            "2015-08-25",
            "You choose how many targets Rolling Thunder has and how the damage is divided as you cast " +
                "the spell. Each target must receive at least 1 damage."
        )
        ruling(
            "2015-08-25",
            "If some, but not all, of the targets become illegal by the time Rolling Thunder tries to " +
                "resolve, the original division of damage still applies, but no damage is dealt to illegal " +
                "targets. If all the targets are illegal, the spell won't resolve."
        )
    }
}
