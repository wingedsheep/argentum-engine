package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

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
        val art = target(TargetFilter(GameObjectFilter.Artifact.youControl()))
        cost = Costs.Tap
        timing = TimingRule.SorcerySpeed
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 3, art) then
            Effects.If(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Noncreature, art),
                then = Effects.BecomeCreature(
                    target = art,
                    power = 0,
                    toughness = 0,
                    creatureTypes = setOf("Robot"),
                    duration = Duration.Permanent
                )
            )
        description = "{T}: Put three +1/+1 counters on target artifact you control. If it isn't a creature, it becomes a 0/0 Robot creature in addition to its other types. Activate only as a sorcery."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Artifact.youControl()).dies()
        interveningIf = Conditions.TriggeringEntityHadCounters
        val dest = target(TargetFilter(GameObjectFilter.CreatureOrArtifact.youControl()), optional = true)
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
