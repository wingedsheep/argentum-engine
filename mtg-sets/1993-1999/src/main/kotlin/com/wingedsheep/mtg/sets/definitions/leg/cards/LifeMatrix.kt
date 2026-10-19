package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Life Matrix — Legends #284
 * {4} · Artifact
 *
 * {4}, {T}: Put a matrix counter on target creature and that creature gains "Remove a matrix
 * counter from this creature: Regenerate this creature." Activate only during your upkeep.
 *
 * The gained ability is a resolved-effect grant with no duration in the text, so it lasts as long
 * as the creature stays on the battlefield ([Duration.Permanent]). It belongs to the creature, so
 * only the creature's controller can activate it (per the 2004-10-04 ruling) — even when Life
 * Matrix targeted an opponent's creature.
 */
val LifeMatrix = card("Life Matrix") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{4}, {T}: Put a matrix counter on target creature and that creature gains " +
        "\"Remove a matrix counter from this creature: Regenerate this creature.\" " +
        "Activate only during your upkeep."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.MATRIX, 1, creature) then
            Effects.GrantActivatedAbility(
                ability = ActivatedAbility(
                    id = AbilityId.next(),
                    cost = Costs.RemoveCounterFromSelf(CounterType.MATRIX),
                    effect = Effects.Regenerate(EffectTarget.Self),
                ),
                target = creature,
                duration = Duration.Permanent,
            )
        restrictions = listOf(
            ActivationRestriction.All(
                ActivationRestriction.OnlyDuringYourTurn,
                ActivationRestriction.DuringStep(Step.UPKEEP)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "284"
        artist = "Amy Weber"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c99a3abc-e2a3-4eee-8f72-b1b25dcd1d0b.jpg?1783948027"
        ruling(
            "2004-10-04",
            "Once the counter is placed on the creature, the ability to remove it can only be activated " +
                "by the player who controls the creature."
        )
    }
}
