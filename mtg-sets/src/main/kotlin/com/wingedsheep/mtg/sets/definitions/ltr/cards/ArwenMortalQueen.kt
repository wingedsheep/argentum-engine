package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Arwen, Mortal Queen
 * {1}{G}{W}
 * Legendary Creature — Elf Noble
 * 2/2
 *
 * Arwen enters with an indestructible counter on it.
 * {1}, Remove an indestructible counter from Arwen: Another target creature gains indestructible
 * until end of turn. Put a +1/+1 counter and a lifelink counter on that creature and a +1/+1
 * counter and a lifelink counter on Arwen.
 */
val ArwenMortalQueen = card("Arwen, Mortal Queen") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Elf Noble"
    power = 2
    toughness = 2
    oracleText = "Arwen enters with an indestructible counter on it.\n" +
        "{1}, Remove an indestructible counter from Arwen: Another target creature gains indestructible " +
        "until end of turn. Put a +1/+1 counter and a lifelink counter on that creature and a +1/+1 counter " +
        "and a lifelink counter on Arwen."

    replacementEffect(EntersWithCounters(
        counterType = CounterTypeFilter.Named(Counters.INDESTRUCTIBLE),
        count = 1,
        selfOnly = true
    ))

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.RemoveCounterFromSelf(Counters.INDESTRUCTIBLE)
        )
        val creature = target(
            "another target creature",
            TargetCreature(filter = TargetFilter.Creature.copy(excludeSelf = true))
        )
        effect = Effects.Composite(
            listOf(
                Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creature),
                AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, creature),
                AddCountersEffect(Counters.LIFELINK, 1, creature),
                AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                AddCountersEffect(Counters.LIFELINK, 1, EffectTarget.Self)
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "193"
        artist = "Miranda Meeks"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/547f92d4-cd1d-4ca7-a6e2-6473b4d3c832.jpg?1686969656"
    }
}
