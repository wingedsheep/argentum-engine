package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Encumbered Reejerey
 * {1}{W}
 * Creature — Merfolk Soldier
 * 5/4
 *
 * This creature enters with three -1/-1 counters on it.
 * Whenever this creature becomes tapped while it has a -1/-1 counter on it,
 * remove a -1/-1 counter from it.
 */
val EncumberedReejerey = card("Encumbered Reejerey") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Merfolk Soldier"
    power = 5
    toughness = 4
    oracleText = "This creature enters with three -1/-1 counters on it.\n" +
        "Whenever this creature becomes tapped while it has a -1/-1 counter on it, " +
        "remove a -1/-1 counter from it."

    replacementEffect(EntersWithCounters(
        counterType = CounterTypeFilter.MinusOneMinusOne,
        count = 3,
        selfOnly = true
    ))

    triggeredAbility {
        trigger = Triggers.BecomesTapped
        triggerCondition = Conditions.SourceHasCounter(CounterTypeFilter.MinusOneMinusOne)
        effect = Effects.RemoveCounters(Counters.MINUS_ONE_MINUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Jeff Miracola"
        flavorText = "A merrow's value to their school is heavily determined by the wealth they're able to bring back."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15ff6797-f59c-4333-98ea-5711150fd5b8.jpg?1767951670"
    }
}
