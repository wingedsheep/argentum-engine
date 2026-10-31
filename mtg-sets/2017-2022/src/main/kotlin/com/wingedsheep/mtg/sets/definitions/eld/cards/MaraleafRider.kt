package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Maraleaf Rider
 * {1}{G}
 * Creature — Elf Knight
 * 3/1
 *
 * Sacrifice a Food: Target creature blocks this creature this turn if able.
 *
 * Same shape as Rampant Elephant: [Effects.ForceBlock] pins the target to blocking this creature.
 */
val MaraleafRider = card("Maraleaf Rider") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Elf Knight"
    power = 3
    toughness = 1
    oracleText = "Sacrifice a Food: Target creature blocks this creature this turn if able."

    activatedAbility {
        val creature = target(TargetFilter.Creature)
        cost = Costs.Sacrifice(GameObjectFilter.Any.withSubtype("Food"))
        effect = Effects.ForceBlock(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "166"
        artist = "Kieran Yanner"
        flavorText = "They both hunger for battle."
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b2b198d-493c-4d6c-bfb6-e842728522f6.jpg?1783932607"
    }
}
