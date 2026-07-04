package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Heartless Act
 * {1}{B}
 * Instant
 * Choose one —
 * • Destroy target creature with no counters on it.
 * • Remove up to three counters from target creature.
 *
 * A true "Choose one —" modal spell ([ModalEffect.chooseOne]). Mode 1 destroys a creature
 * constrained to having no counters via [TargetFilter.withoutCounters] (composed from
 * `StatePredicate.Not(HasAnyCounter)`). Mode 2 uses [Effects.RemoveCountersUpTo], the
 * total-budget-capped counter-removal effect: the controller chooses how many counters of each
 * kind to remove from the target, capped at three counters in total.
 */
val HeartlessAct = card("Heartless Act") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Destroy target creature with no counters on it.\n" +
        "• Remove up to three counters from target creature."

    spell {
        effect = ModalEffect.chooseOne(
            Mode(
                effect = Effects.Destroy(EffectTarget.ContextTarget(0)),
                targetRequirements = listOf(
                    TargetObject(
                        filter = TargetFilter.Creature.withoutCounters(),
                        id = "target creature with no counters on it",
                    ),
                ),
                description = "Destroy target creature with no counters on it",
            ),
            Mode(
                effect = Effects.RemoveCountersUpTo(3, EffectTarget.ContextTarget(0)),
                targetRequirements = listOf(
                    TargetObject(
                        filter = TargetFilter.Creature,
                        id = "target creature",
                    ),
                ),
                description = "Remove up to three counters from target creature",
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "103"
        artist = "Sylvain Sarrailh"
        flavorText = "\"Put a muzzle on him!\"\n—Si Wong Sandbender"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b57c57a8-d72b-4c6c-a2db-a7ef61190f42.jpg?1764120715"
    }
}
