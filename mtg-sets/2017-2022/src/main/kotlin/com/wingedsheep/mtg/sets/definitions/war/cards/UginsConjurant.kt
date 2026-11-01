package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CounterRemovalAmount
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.PreventDamageByRemovingCounter

/**
 * Ugin's Conjurant
 * {X}
 * Creature — Spirit Monk
 * 0/0
 *
 * This creature enters with X +1/+1 counters on it.
 * If damage would be dealt to this creature while it has a +1/+1 counter on it, prevent that
 * damage and remove that many +1/+1 counters from this creature.
 *
 * The same shield as Magma Pummeler without its reflexive payoff: `requiresCounter = true` is
 * "while it has a +1/+1 counter on it", and `EqualToDamage` is "that many", bounded by the counters
 * present — damage beyond the count is still prevented in full (per the ruling).
 */
val UginsConjurant = card("Ugin's Conjurant") {
    manaCost = "{X}"
    colorIdentity = ""
    typeLine = "Creature — Spirit Monk"
    power = 0
    toughness = 0
    oracleText = "This creature enters with X +1/+1 counters on it.\n" +
        "If damage would be dealt to this creature while it has a +1/+1 counter on it, prevent " +
        "that damage and remove that many +1/+1 counters from this creature."

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.xValue()))

    replacementEffect(
        PreventDamageByRemovingCounter(
            removalAmount = CounterRemovalAmount.EqualToDamage,
            requiresCounter = true
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "3"
        artist = "Ryan Yee"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/9957ecca-5ea0-446f-bfd3-46c2aaa42f08.jpg?1783933486"
        ruling("2019-05-03", "To cast Ugin's Conjurant, first choose the value for X you'd like, then apply any cost increases, then any cost reductions. The number of counters it enters with may be different than the amount of mana you pay if an effect (such as that of Ugin, the Ineffable) causes that you pay an amount of mana other than {X}.")
        ruling("2019-05-03", "If Ugin's Conjurant would be dealt more damage than it has counters, you remove all of the +1/+1 counters it has on it and prevent all of that damage. If another effect is raising its toughness, it will survive until the next time it's dealt lethal damage.")
    }
}
