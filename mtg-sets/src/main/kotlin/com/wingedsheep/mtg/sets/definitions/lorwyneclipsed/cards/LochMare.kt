package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter

/**
 * Loch Mare
 * {1}{U}
 * Creature — Horse Serpent
 * 4/5
 *
 * This creature enters with three -1/-1 counters on it.
 * {1}{U}, Remove a counter from this creature: Draw a card.
 * {2}{U}, Remove two counters from this creature: Tap target creature. Put a
 * stun counter on it.
 */
val LochMare = card("Loch Mare") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Horse Serpent"
    power = 4
    toughness = 5
    oracleText = "This creature enters with three -1/-1 counters on it.\n" +
        "{1}{U}, Remove a counter from this creature: Draw a card.\n" +
        "{2}{U}, Remove two counters from this creature: Tap target creature. " +
        "Put a stun counter on it. (If a permanent with a stun counter would become untapped, remove one from it instead.)"

    replacementEffect(EntersWithCounters(
        counterType = CounterTypeFilter.MinusOneMinusOne,
        count = 3,
        selfOnly = true
    ))

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{U}"),
            Costs.RemoveCounterFromSelf(Counters.MINUS_ONE_MINUS_ONE)
        )
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{U}"),
            Costs.RemoveCounterFromSelf(Counters.MINUS_ONE_MINUS_ONE, 2)
        )
        val creature = target("creature", Targets.Creature)
        effect = Effects.Tap(creature)
            .then(Effects.AddCounters(Counters.STUN, 1, creature))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "57"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad6c4baf-a803-45e4-81ac-708a41631a28.jpg?1767862352"
    }
}
