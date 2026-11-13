package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Roil's Retribution
 * {3}{W}{W}
 * Instant
 * Roil's Retribution deals 5 damage divided as you choose among any number of target attacking or
 * blocking creatures.
 *
 * Spreading Flames' shape restricted to attacking or blocking creatures: each target must be
 * assigned at least 1 damage, so "any number" caps at five targets, and the division is announced
 * as the spell is cast.
 */
val RoilsRetribution = card("Roil's Retribution") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Roil's Retribution deals 5 damage divided as you choose among any number of target " +
        "attacking or blocking creatures."

    spell {
        target = TargetObject(filter = TargetFilter.AttackingOrBlockingCreature, count = 5, minCount = 1)
        effect = Effects.DividedDamage(total = 5, minTargets = 1, maxTargets = 5)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "45"
        artist = "Raymond Swanland"
        flavorText = "\"We can use their hunger to lead them into danger. If we're already prey, we may as " +
            "well be bait!\"\n—Yon Basrel, Oran-Rief survivalist"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0de575e-d955-48da-8fc8-05d7398bd261.jpg?1783938215"
        ruling(
            "2015-08-25",
            "You choose how many targets Roil's Retribution has and how the damage is divided as you cast " +
                "the spell. Each target must receive at least 1 damage."
        )
        ruling(
            "2015-08-25",
            "If some, but not all, of the targets become illegal by the time Roil's Retribution tries to " +
                "resolve, the original division of damage still applies, but no damage is dealt to illegal " +
                "targets. If all the targets are illegal, the spell won't resolve."
        )
    }
}
