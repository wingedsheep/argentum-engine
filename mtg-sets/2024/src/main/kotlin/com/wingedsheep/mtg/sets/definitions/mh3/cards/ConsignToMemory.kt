package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Consign to Memory
 * {U}
 * Instant
 * Replicate {1}
 * Counter target triggered ability or colorless spell.
 *
 * An ability on the stack has no card characteristics, so `IsColorless` only ever matches a spell
 * and the `Or` reads exactly as printed. Each replicate copy may pick its own target.
 */
val ConsignToMemory = card("Consign to Memory") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Replicate {1} (When you cast this spell, copy it for each time you paid its replicate cost. " +
        "You may choose new targets for the copies.)\n" +
        "Counter target triggered ability or colorless spell."

    keywordAbility(KeywordAbility.replicate("{1}"))

    spell {
        target(
            TargetFilter(
                GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.Or(listOf(CardPredicate.IsTriggeredAbility, CardPredicate.IsColorless))
                    )
                ),
                zone = Zone.STACK
            )
        )
        effect = Effects.CounterSpellOrAbility()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Ben Hill"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc95af55-d1dd-4fe6-adb0-3ad6db20d986.jpg?1783911293"
        ruling(
            "2024-06-07",
            "A copy of a spell can be countered like any other spell, but it must be countered individually. " +
                "Countering a spell with replicate won't affect the copies."
        )
    }
}
