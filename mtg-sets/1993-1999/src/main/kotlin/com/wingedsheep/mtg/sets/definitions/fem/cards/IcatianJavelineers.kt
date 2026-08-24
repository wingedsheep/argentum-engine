package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Icatian Javelineers
 * {W}
 * Creature — Human Soldier
 * 1/1
 * This creature enters with a javelin counter on it.
 * {T}, Remove a javelin counter from this creature: It deals 1 damage to any target.
 *
 * Triskelion's shape with a named counter: the counter is a one-shot charge, so the ping is
 * available exactly once unless something else puts javelin counters on it.
 */
val IcatianJavelineers = card("Icatian Javelineers") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "This creature enters with a javelin counter on it.\n" +
        "{T}, Remove a javelin counter from this creature: It deals 1 damage to any target."
    power = 1
    toughness = 1

    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.Named(Counters.JAVELIN),
            count = 1,
            selfOnly = true
        )
    )

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.RemoveCounterFromSelf(Counters.JAVELIN, 1))
        val t = target("any target", AnyTarget())
        effect = Effects.DealDamage(1, t)
        description = "{T}, Remove a javelin counter from this creature: It deals 1 damage to any target."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "8a"
        artist = "Melissa A. Benson"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f04b8356-2384-4743-80dd-f15ca7ec65f7.jpg?1783947919"
    }
}
