package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scourge of Skola Vale
 * {2}{G}
 * Creature — Hydra
 * 0/0
 *
 * Trample
 * This creature enters with two +1/+1 counters on it.
 * {T}, Sacrifice another creature: Put a number of +1/+1 counters on this creature equal to the
 * sacrificed creature's toughness.
 *
 * The sacrifice is an activation cost ([Costs.SacrificeAnother], so the Hydra can't eat itself),
 * and [DynamicAmounts.sacrificedToughness] reads the cost-sacrificed creature's toughness as it
 * last existed on the battlefield — pumps and counters it had at the moment of sacrifice count.
 */
val ScourgeOfSkolaVale = card("Scourge of Skola Vale") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Hydra"
    power = 0
    toughness = 0
    oracleText = "Trample\n" +
        "This creature enters with two +1/+1 counters on it.\n" +
        "{T}, Sacrifice another creature: Put a number of +1/+1 counters on this creature equal to " +
        "the sacrificed creature's toughness."

    keywords(Keyword.TRAMPLE)

    replacementEffect(EntersWithCounters(count = 2, selfOnly = true))

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.SacrificeAnother(GameObjectFilter.Creature),
        )
        effect = Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.sacrificedToughness(),
            EffectTarget.Self,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "137"
        artist = "Dave Kendall"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/baef76dc-7cef-449b-b8a7-e038887129d9.jpg?1783939529"
        ruling(
            "2014-02-01",
            "Use the toughness of the sacrificed creature as it last existed on the battlefield to " +
                "determine how many +1/+1 counters to put on Scourge of Skola Vale."
        )
    }
}
