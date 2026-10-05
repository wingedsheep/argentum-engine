package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Conductor of Cacophony
 * {3}{B}
 * Creature — Demon
 * 2/1
 * This creature enters with two +1/+1 counters on it.
 * {B}, Remove a +1/+1 counter from this creature: It deals 1 damage to each other creature and
 * each player.
 *
 * "Each other creature and each player" is two passes, as on Thrashing Wumpus: a group pass over
 * the other creatures and a player pass that rebinds the controller per iteration.
 */
val ConductorOfCacophony = card("Conductor of Cacophony") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 2
    toughness = 1
    oracleText = "This creature enters with two +1/+1 counters on it.\n" +
        "{B}, Remove a +1/+1 counter from this creature: It deals 1 damage to each other creature and each player."

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 2,
        selfOnly = true
    ))

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{B}"),
            Costs.RemoveCounterFromSelf(CounterType.PLUS_ONE_PLUS_ONE, 1)
        )
        effect = Effects.ForEachInGroup(
            GroupFilter.AllOtherCreatures,
            Effects.DealDamage(1, EffectTarget.IterationEntity)
        ) then
            Effects.ForEachPlayer(
                Player.Each,
                listOf(Effects.DealDamage(1, EffectTarget.Controller))
            )
        description = "{B}, Remove a +1/+1 counter from this creature: It deals 1 damage to each other creature and each player."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "20"
        artist = "Jason A. Engle"
        flavorText = "Only an imitation of tortured shrieking\n—or so the Rakdos claim."
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37d4f19b-3c4e-4aa0-90f5-6cf397a5ad6d.jpg?1783919189"
    }
}
