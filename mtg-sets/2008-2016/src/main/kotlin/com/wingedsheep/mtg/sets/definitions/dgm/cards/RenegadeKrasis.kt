package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.evolve
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Renegade Krasis — Dragon's Maze #47 (canonical printing)
 * {1}{G}{G} · Creature — Beast Mutant · 3/2
 *
 * "Whenever this creature evolves" is [Triggers.self]`.evolves()` (Watchful Radstag's shape): the
 * `evolve()` ability emits an `EvolvedEvent` only when its own +1/+1 counter lands (CR 702.100b), so
 * counters from other sources don't trigger it and a Doubling Season-style extra counter triggers it
 * once. The payoff is Patron of the Valiant's group shape with `excludeSelf` for "each other".
 */
val RenegadeKrasis = card("Renegade Krasis") {
    manaCost = "{1}{G}{G}"
    typeLine = "Creature — Beast Mutant"
    power = 3
    toughness = 2
    oracleText = "Evolve (Whenever a creature you control enters, if that creature has greater power or toughness than this creature, put a +1/+1 counter on this creature.)\n" +
        "Whenever this creature evolves, put a +1/+1 counter on each other creature you control with a +1/+1 counter on it."

    evolve()

    triggeredAbility {
        trigger = Triggers.self.evolves()
        effect = Effects.ForEachInGroup(
            GroupFilter(
                GameObjectFilter.Creature.withCounter(CounterType.PLUS_ONE_PLUS_ONE).youControl(),
                excludeSelf = true,
            ),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
        description = "Whenever this creature evolves, put a +1/+1 counter on each other creature you control " +
            "with a +1/+1 counter on it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "47"
        artist = "Howard Lyon"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23b68921-0c34-4d92-83c3-21542f62c7f6.jpg?1783940034"
        ruling("2013-04-15", "Renegade Krasis “evolves” when its evolve ability resolves and a +1/+1 counter is put on it. If a replacement effect such as Doubling Season’s causes the evolve ability to put more than one +1/+1 counter on Renegade Krasis, its last ability triggers only once. If no +1/+1 counter is put on it (perhaps because it left the battlefield in response to its evolve ability triggering), then its last ability doesn’t trigger.")
        ruling("2013-04-15", "Renegade Krasis’s last ability won’t trigger if a +1/+1 counter is put on it for any reason other than its evolve ability resolving.")
    }
}
