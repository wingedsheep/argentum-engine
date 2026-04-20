package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Moonshadow
 * {B}
 * Creature — Elemental
 * 7/7
 *
 * Menace
 * This creature enters with six -1/-1 counters on it.
 * Whenever one or more permanent cards are put into your graveyard from anywhere
 * while this creature has a -1/-1 counter on it, remove a -1/-1 counter from this creature.
 */
val Moonshadow = card("Moonshadow") {
    manaCost = "{B}"
    typeLine = "Creature — Elemental"
    power = 7
    toughness = 7
    oracleText = "Menace\n" +
        "This creature enters with six -1/-1 counters on it.\n" +
        "Whenever one or more permanent cards are put into your graveyard from anywhere " +
        "while this creature has a -1/-1 counter on it, remove a -1/-1 counter from this creature."

    keywords(Keyword.MENACE)

    replacementEffect(EntersWithCounters(
        counterType = CounterTypeFilter.MinusOneMinusOne,
        count = 6,
        selfOnly = true
    ))

    triggeredAbility {
        trigger = Triggers.PermanentCardsPutIntoYourGraveyard
        triggerCondition = Conditions.SourceHasCounter(CounterTypeFilter.MinusOneMinusOne)
        effect = Effects.RemoveCounters(Counters.MINUS_ONE_MINUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "110"
        artist = "Olivier Bernard"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/2573e694-eaa0-42ca-b470-2ab507cbcec1.jpg?1767952044"
    }
}
