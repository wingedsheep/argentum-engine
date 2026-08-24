package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Armor Thrull
 * {2}{B}
 * Creature — Thrull
 * 1/3
 * {T}, Sacrifice this creature: Put a +1/+2 counter on target creature.
 *
 * A +1/+2 counter is an ordinary +X/+Y counter (CR 122.1a); the engine gained the kind for this
 * card, alongside Soul Exchange's +2/+2 and Ebon Praetor's -2/-2.
 */
val ArmorThrull = card("Armor Thrull") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Thrull"
    oracleText = "{T}, Sacrifice this creature: Put a +1/+2 counter on target creature."
    power = 1
    toughness = 3

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        val t = target("target creature", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_TWO, 1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33a"
        artist = "Pete Venters"
        flavorText = "\"The worst thing about being a mercenary for the Ebon Hand is having to wear a dead Thrull.\"\n—Ivra Jursdotter"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a98384d1-8e7d-4c41-9f23-47bc2ae2ad6a.jpg?1783947906"
    }
}
