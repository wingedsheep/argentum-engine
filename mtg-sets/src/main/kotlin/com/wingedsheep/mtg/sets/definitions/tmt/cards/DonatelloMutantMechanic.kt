package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Donatello, Mutant Mechanic
 * {3}{U}
 * Legendary Creature — Mutant Ninja Turtle
 * 3/5
 *
 * {T}: Put three +1/+1 counters on target artifact you control. If it isn't a
 * creature, it becomes a 0/0 Robot creature in addition to its other types.
 * Activate only as a sorcery.
 * Whenever an artifact you control is put into a graveyard from the battlefield, if
 * it had counters on it, put those counters on up to one target artifact or creature
 * you control.
 */
val DonatelloMutantMechanic = card("Donatello, Mutant Mechanic") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Mutant Ninja Turtle"
    oracleText = "{T}: Put three +1/+1 counters on target artifact you control. If it isn't a creature, it becomes a 0/0 Robot creature in addition to its other types. Activate only as a sorcery.\nWhenever an artifact you control is put into a graveyard from the battlefield, if it had counters on it, put those counters on up to one target artifact or creature you control."
    power = 3
    toughness = 5

    activatedAbility {
        val art = target(
            "target artifact you control",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.Artifact.youControl()))
        )
        cost = Costs.Tap
        timing = TimingRule.SorcerySpeed
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 3, art)
            .then(
                ConditionalEffect(
                    condition = Conditions.TargetMatchesFilter(GameObjectFilter.Noncreature),
                    effect = Effects.BecomeCreature(
                        target = EffectTarget.ContextTarget(0),
                        power = 0,
                        toughness = 0,
                        creatureTypes = setOf("Robot"),
                        duration = Duration.Permanent
                    )
                )
            )
        description = "{T}: Put three +1/+1 counters on target artifact you control. If it isn't a creature, it becomes a 0/0 Robot creature in addition to its other types. Activate only as a sorcery."
    }

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(filter = GameObjectFilter.Artifact.youControl(), to = Zone.GRAVEYARD)
        triggerCondition = Conditions.TriggeringEntityHadCounters
        val dest = target(
            "up to one target artifact or creature you control",
            TargetPermanent(optional = true, filter = TargetFilter(GameObjectFilter.CreatureOrArtifact.youControl()))
        )
        effect = Effects.MoveAllLastKnownCounters(dest)
        description = "Whenever an artifact you control is put into a graveyard from the battlefield, if it had counters on it, put those counters on up to one target artifact or creature you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "36"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/3271b821-8efc-49e2-96fd-c48e2b2585c6.jpg?1769005688"
    }
}
