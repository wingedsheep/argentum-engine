package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scale Blessing
 * {3}{W}
 * Instant
 *
 * Bolster 1, then put a +1/+1 counter on each creature you control with a +1/+1 counter on it.
 * (To bolster 1, choose a creature with the least toughness among creatures you control and put a
 * +1/+1 counter on it.)
 *
 * The bolster runs first; the group is gathered only when the second instruction resolves, so the
 * bolstered creature (which now has a counter) gets a second one.
 */
val ScaleBlessing = card("Scale Blessing") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Bolster 1, then put a +1/+1 counter on each creature you control with a +1/+1 counter " +
        "on it. (To bolster 1, choose a creature with the least toughness among creatures you control and " +
        "put a +1/+1 counter on it.)"

    spell {
        effect = Patterns.Mechanic.bolster(1) then Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withCounter(CounterType.PLUS_ONE_PLUS_ONE).youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Matt Stewart"
        flavorText = "\"Your bravery honors us all.\"\n—Dromoka, translated from Draconic"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a76515a0-31eb-4bba-bf07-1f9fde423e24.jpg?1783938613"
        ruling(
            "2015-02-25",
            "You determine which creature to put counters on as the spell or ability that instructs you to " +
                "bolster resolves.",
        )
    }
}
