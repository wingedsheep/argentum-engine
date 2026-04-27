package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter

val FlitterwingNuisance = card("Flitterwing Nuisance") {
    manaCost = "{U}"
    typeLine = "Creature — Faerie Rogue"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "This creature enters with a -1/-1 counter on it.\n" +
        "{2}{U}, Remove a counter from this creature: Whenever a creature you control deals " +
        "combat damage to a player or planeswalker this turn, draw a card."

    keywords(Keyword.FLYING)

    replacementEffect(EntersWithCounters(
        counterType = CounterTypeFilter.MinusOneMinusOne,
        count = 1,
        selfOnly = true
    ))

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{U}"),
            Costs.RemoveCounterFromSelf(Counters.MINUS_ONE_MINUS_ONE)
        )
        effect = CreateDelayedTriggerEffect(
            trigger = Triggers.CreatureYouControlDealsCombatDamageToPlayer,
            effect = Effects.DrawCards(1),
            expiry = DelayedTriggerExpiry.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "48"
        artist = "Evyn Fong"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad0f6536-5295-4835-8883-35d711dfe6de.jpg?1767862448"
    }
}
