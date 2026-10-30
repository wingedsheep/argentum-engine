package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Blood Curdle
 * {3}{B}
 * Instant
 * Destroy target creature. Put a menace counter on a creature you control.
 *
 * Only the destroyed creature is targeted. The creature that gets the menace counter is chosen on
 * resolution, after the destruction (2020-04-17 ruling) — a non-targeting `selectTarget`
 * (Humiliate's shape) — and the counter is still placed when the target survives (indestructible).
 */
val BloodCurdle = card("Blood Curdle") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature. Put a menace counter on a creature you control. " +
        "(It can't be blocked except by two or more creatures.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Pipeline {
            run(Effects.Destroy(creature))
            val chosen = selectTarget(
                TargetObject(filter = TargetFilter.CreatureYouControl),
                nonTargeting = true,
                prompt = "Choose a creature you control to get a menace counter"
            )
            run(Effects.AddCounters(CounterType.MENACE, 1, chosen.asTarget))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Antonio José Manzanedo"
        flavorText = "Fear pierces the strongest armor."
        imageUri = "https://cards.scryfall.io/normal/front/4/1/4184c851-1419-476c-ba9c-9f0cb1137114.jpg?1783931065"
        ruling("2020-04-17", "Blood Curdle targets only one creature: the one that will be destroyed. You choose which creature to put a menace counter on after the target creature has been destroyed.")
        ruling("2020-04-17", "If the target creature is an illegal target by the time Blood Curdle tries to resolve, the spell won't resolve. You won't put a menace counter on any creature. If the target is legal but not destroyed (most likely because it has indestructible), you do put a menace counter on a creature you control.")
    }
}
