package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sentinel
 * {4}
 * Artifact Creature — Shapeshifter
 * 1/1
 *
 * {0}: This creature's base toughness becomes equal to 1 plus the power of target creature
 * blocking or blocked by this creature. (This effect lasts indefinitely.)
 *
 * Layer 7b set-base-toughness on Sentinel itself, with the amount frozen at resolution
 * (`reevaluateContinuously = false`) and `Duration.Permanent` — "lasts indefinitely". The target
 * is restricted to creatures combat-paired with the source (blocking it or blocked by it).
 */
val Sentinel = card("Sentinel") {
    manaCost = "{4}"
    typeLine = "Artifact Creature — Shapeshifter"
    power = 1
    toughness = 1
    oracleText = "{0}: This creature's base toughness becomes equal to 1 plus the power of target " +
        "creature blocking or blocked by this creature. (This effect lasts indefinitely.)"

    activatedAbility {
        cost = Costs.Free
        val opponent = target(TargetFilter(GameObjectFilter.Creature.blockingOrBlockedBySource()))
        effect = Effects.SetBaseToughness(
            target = EffectTarget.Self,
            toughness = DynamicAmounts.powerOf(opponent) + 1
        )
        description = "Sentinel's base toughness becomes 1 plus the power of target creature " +
            "blocking or blocked by it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "294"
        artist = "Randy Asplund-Faith"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c970830-95cf-4471-af28-43da635073d0.jpg?1783948025"
    }
}
