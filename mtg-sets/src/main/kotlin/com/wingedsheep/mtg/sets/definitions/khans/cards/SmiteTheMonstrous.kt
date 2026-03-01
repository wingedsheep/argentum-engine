package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Smite the Monstrous
 * {3}{W}
 * Instant
 * Destroy target creature with power 4 or greater.
 */
val SmiteTheMonstrous = card("Smite the Monstrous") {
    manaCost = "{3}{W}"
    typeLine = "Instant"
    oracleText = "Destroy target creature with power 4 or greater."

    spell {
        val t = target("target", TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.powerAtLeast(4))))
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Greg Staples"
        flavorText = "\"The dragons thought they were too strong to be tamed, too large to fall. And where are they now?\" —Khibat the Revered"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/1405bb2e-2204-43ab-82a3-5d0c8537325a.jpg?1562782881"
    }
}
