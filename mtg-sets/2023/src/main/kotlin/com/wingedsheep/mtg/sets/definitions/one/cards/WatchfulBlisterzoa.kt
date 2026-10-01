package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters

/**
 * Watchful Blisterzoa
 * {4}{U}{U}
 * Creature — Phyrexian Jellyfish
 * 4/4
 *
 * Flying
 * This creature enters with an oil counter on it.
 * When this creature dies, draw cards equal to the number of oil counters on it.
 *
 * The dies trigger reads the oil-counter count as the creature last existed on the battlefield
 * (last-known information) — the card in the graveyard has no counters.
 */
val WatchfulBlisterzoa = card("Watchful Blisterzoa") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Phyrexian Jellyfish"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "This creature enters with an oil counter on it.\n" +
        "When this creature dies, draw cards equal to the number of oil counters on it."

    keywords(Keyword.FLYING)

    replacementEffect(EntersWithCounters(counterType = CounterType.OIL, count = 1, selfOnly = true))

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.DrawCards(DynamicAmounts.lastKnownSourceCounters(CounterType.OIL))
        description = "When this creature dies, draw cards equal to the number of oil counters on it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Chris Cold"
        flavorText = "After a series of ambushes, Uulbrek demanded a sentry with no blind spots."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a895f63f-3c59-4249-9346-55ef489944fc.jpg?1783918053"
    }
}
