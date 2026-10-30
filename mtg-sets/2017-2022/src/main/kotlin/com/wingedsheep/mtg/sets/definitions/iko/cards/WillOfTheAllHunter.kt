package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Will of the All-Hunter — Ikoria: Lair of Behemoths #38
 * {1}{W} · Instant
 *
 * Target creature gets +2/+2 until end of turn. If it's blocking, instead put two +1/+1 counters
 * on it.
 * Cycling {2} ({2}, Discard this card: Draw a card.)
 *
 * "Instead" is an either/or on resolution: [Effects.If] checks whether the target is blocking
 * (live combat state, via the `blocking()` state predicate) and picks the counters branch, otherwise
 * the end-of-turn pump. The two never both apply.
 */
val WillOfTheAllHunter = card("Will of the All-Hunter") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 until end of turn. If it's blocking, instead put two " +
        "+1/+1 counters on it.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.EntityMatches(
                creature,
                GameObjectFilter.Creature.blocking(),
            ),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature),
            otherwise = Effects.ModifyStats(2, 2, creature),
        )
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Viktor Titov"
        flavorText = "\"It's Snapdax. You'd have a better chance of breaching a wall with a slice of bread.\"\n—Krek, daysquad captain"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa7acbfa-ce3b-424d-886d-6865bc5fc085.jpg?1783931080"
    }
}
