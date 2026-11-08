package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.IncrementAbilityResolutionCountEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scythecat Cub — Foundations Jumpstart #24 (canonical printing; its debut)
 * {1}{G} · Creature — Cat · 2/2
 *
 * Trample
 * Landfall — Whenever a land you control enters, put a +1/+1 counter on target creature you control.
 * If this is the second time this ability has resolved this turn, double the number of +1/+1
 * counters on that creature instead.
 *
 * The landfall payoff bumps the source's per-turn resolution counter first and branches on it
 * (cf. Omnath, Locus of Creation): the second resolution doubles the target's +1/+1 counters *instead
 * of* adding one, so a target with none stays at none; every other resolution adds one. A fizzled
 * trigger never resolves, so it doesn't advance the count.
 */
val ScythecatCub = card("Scythecat Cub") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat"
    oracleText = "Trample\n" +
        "Landfall — Whenever a land you control enters, put a +1/+1 counter on target creature you " +
        "control. If this is the second time this ability has resolved this turn, double the number of " +
        "+1/+1 counters on that creature instead."
    power = 2
    toughness = 2

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = IncrementAbilityResolutionCountEffect then
            Effects.If(
                condition = Conditions.SourceAbilityResolvedNTimes(2),
                then = Effects.DoubleCounters(CounterType.PLUS_ONE_PLUS_ONE, creature),
                otherwise = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature),
            )
        description = "Landfall — Whenever a land you control enters, put a +1/+1 counter on target " +
            "creature you control. If this is the second time this ability has resolved this turn, " +
            "double the number of +1/+1 counters on that creature instead."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Gabor Szikszai"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3dd3c7d-4685-4579-b483-14ddaaaddf5b.jpg?1783908863"
        ruling("2024-11-08", "To double the number of +1/+1 counters on a creature, put a number of +1/+1 counters on it equal to the number it already has. Other cards that interact with putting counters on it will interact with this effect accordingly.")
    }
}
