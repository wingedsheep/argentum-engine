package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Mogg Mob
 * {R}{R}{R}
 * Creature — Goblin
 * 3/3
 * Sacrifice this creature: It deals 3 damage divided as you choose among one, two, or three targets.
 *
 * The division is announced with the activation (CR 601.2d); the sacrificed Mob is still the damage
 * source, read through last-known information.
 */
val MoggMob = card("Mogg Mob") {
    manaCost = "{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    oracleText = "Sacrifice this creature: It deals 3 damage divided as you choose among one, two, or three targets."
    power = 3
    toughness = 3

    activatedAbility {
        cost = Costs.SacrificeSelf
        target = AnyTarget(count = 3, minCount = 1)
        effect = Effects.DividedDamage(
            total = 3,
            minTargets = 1,
            maxTargets = 3
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Joseph Weston"
        flavorText = "There was absolutely nothing suspicious about three moggs carrying barrels of explosives—or so they told everyone who would listen."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1457aa8-9f70-4586-b595-6a722879f6ae.jpg?1783911270"
    }
}
