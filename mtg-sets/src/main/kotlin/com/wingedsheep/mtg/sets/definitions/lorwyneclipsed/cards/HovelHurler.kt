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
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Hovel Hurler
 * {3}{R/W}{R/W}
 * Creature — Giant Warrior
 * 6/7
 *
 * This creature enters with two -1/-1 counters on it.
 * {R/W}{R/W}, Remove a counter from this creature: Another target creature you
 * control gets +1/+0 and gains flying until end of turn. Activate only as a sorcery.
 */
val HovelHurler = card("Hovel Hurler") {
    manaCost = "{3}{R/W}{R/W}"
    typeLine = "Creature — Giant Warrior"
    power = 6
    toughness = 7
    oracleText = "This creature enters with two -1/-1 counters on it.\n{R/W}{R/W}, Remove a counter from this creature: Another target creature you control gets +1/+0 and gains flying until end of turn. Activate only as a sorcery."

    replacementEffect(EntersWithCounters(
        counterType = CounterTypeFilter.MinusOneMinusOne,
        count = 2,
        selfOnly = true
    ))

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{R/W}{R/W}"),
            Costs.RemoveCounterFromSelf(Counters.MINUS_ONE_MINUS_ONE)
        )
        val creature = target("another creature you control", TargetCreature(filter = TargetFilter.OtherCreatureYouControl))
        effect = Effects.ModifyStats(1, 0, creature)
            .then(Effects.GrantKeyword(Keyword.FLYING, creature))
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "230"
        artist = "Chris Seaman"
        flavorText = "Giants love two things: sleeping, and getting rid of things that wake them up."
        imageUri = "https://cards.scryfall.io/normal/front/a/d/adc6a4c5-4e92-43ee-8d0c-204042965eb7.jpg?1767774270"
    }
}
