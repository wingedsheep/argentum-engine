package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Triskelion
 * {6}
 * Artifact Creature — Construct
 * 1/1
 * This creature enters with three +1/+1 counters on it.
 * Remove a +1/+1 counter from this creature: It deals 1 damage to any target.
 */
val Triskelion = card("Triskelion") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 1
    toughness = 1
    oracleText = "This creature enters with three +1/+1 counters on it.\n" +
        "Remove a +1/+1 counter from this creature: It deals 1 damage to any target."

    replacementEffect(EntersWithCounters(
        counterType = CounterTypeFilter.PlusOnePlusOne,
        count = 3,
        selfOnly = true
    ))

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(Counters.PLUS_ONE_PLUS_ONE, 1)
        val t = target("any target", AnyTarget())
        effect = Effects.DealDamage(1, t)
        description = "Remove a +1/+1 counter from this creature: It deals 1 damage to any target."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "73"
        artist = "Douglas Shuler"
        flavorText = "A brainchild of Tawnos, the Triskelion proved its versatility and usefulness in many of the later battles between the brothers."
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a79c99e1-722a-44b6-8fa3-2be3f0c193d8.jpg?1562930328"
    }
}
