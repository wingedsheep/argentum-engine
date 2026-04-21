package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Burdened Stoneback
 * {1}{W}
 * Creature — Giant Warrior
 * 4/4
 *
 * This creature enters with two -1/-1 counters on it.
 * {1}{W}, Remove a counter from this creature: Target creature gains indestructible
 * until end of turn. Activate only as a sorcery.
 */
val BurdenedStoneback = card("Burdened Stoneback") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Giant Warrior"
    power = 4
    toughness = 4
    oracleText = "This creature enters with two -1/-1 counters on it.\n" +
        "{1}{W}, Remove a counter from this creature: Target creature gains indestructible " +
        "until end of turn. Activate only as a sorcery."

    replacementEffect(EntersWithCounters(
        counterType = CounterTypeFilter.MinusOneMinusOne,
        count = 2,
        selfOnly = true
    ))

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{W}"),
            Costs.RemoveCounterFromSelf(Counters.MINUS_ONE_MINUS_ONE)
        )
        val creature = target("target creature to gain indestructible", TargetCreature())
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creature)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "8"
        artist = "Carl Critchlow"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/3278b8d0-3d2b-4d3d-bbf1-fd9b714b53ed.jpg?1767732463"
    }
}
