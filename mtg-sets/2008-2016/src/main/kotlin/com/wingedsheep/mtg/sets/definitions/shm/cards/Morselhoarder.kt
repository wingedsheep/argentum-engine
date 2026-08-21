package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Morselhoarder
 * {4}{R/G}{R/G}
 * Creature — Elemental
 * 6 / 4
 *
 * This creature enters with two -1/-1 counters on it.
 * Remove a -1/-1 counter from this creature: Add one mana of any color.
 *
 * - The printed 6/4 is the real base P/T; the two -1/-1 counters are an [EntersWithCounters]
 *   replacement (`selfOnly = true`), which is what makes it a 4/2 on the battlefield. Removing a
 *   counter to pay the mana ability grows the body back, so the counters are the ability's fuel
 *   rather than a permanent drawback (the Wickerbough Elder idiom).
 * - The activation has no mana in its cost, only [Costs.RemoveCounterFromSelf], so the whole ability
 *   is a mana ability: it doesn't use the stack and can't be responded to. `manaAbility = true` plus
 *   [TimingRule.ManaAbility] is what tells the engine that.
 */
val Morselhoarder = card("Morselhoarder") {
    manaCost = "{4}{R/G}{R/G}"
    typeLine = "Creature — Elemental"
    power = 6
    toughness = 4
    oracleText = "This creature enters with two -1/-1 counters on it.\n" +
        "Remove a -1/-1 counter from this creature: Add one mana of any color."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.MinusOneMinusOne,
            count = 2,
            selfOnly = true
        )
    )

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(Counters.MINUS_ONE_MINUS_ONE)
        effect = AddManaOfChoiceEffect(ManaColorSet.AnyColor, 1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "212"
        artist = "Anthony S. Waters"
        flavorText = "It scours the hills for living matter, savoring even the tang of poisonous fungi."
        imageUri = "https://cards.scryfall.io/normal/front/5/8/589f477e-fd69-4410-b9ba-1d45b25fec31.jpg?1783942721"
    }
}
