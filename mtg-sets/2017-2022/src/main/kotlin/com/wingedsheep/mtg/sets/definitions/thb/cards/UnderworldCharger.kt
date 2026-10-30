package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Underworld Charger
 * {2}{B}
 * Creature — Nightmare Horse
 * 3/3
 * This creature can't block.
 * Escape—{4}{B}, Exile three other cards from your graveyard.
 * This creature escapes with two +1/+1 counters on it.
 *
 * Kyren Glider's static [CantBlock] plus Pharika's Spawn's escape: [KeywordAbility.escape] and an
 * [EntersWithCounters] replacement gated on [Conditions.Escaped].
 */
val UnderworldCharger = card("Underworld Charger") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Nightmare Horse"
    power = 3
    toughness = 3
    oracleText = "This creature can't block.\n" +
        "Escape—{4}{B}, Exile three other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)\n" +
        "This creature escapes with two +1/+1 counters on it."

    staticAbility {
        ability = CantBlock()
    }

    keywordAbility(KeywordAbility.escape("{4}{B}", Costs.additional.ExileOtherCards(3)))

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 2,
        selfOnly = true,
        condition = Conditions.Escaped
    ))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2dd847f-0db2-4f6a-bdfb-5c88ce7802f9.jpg?1783931558"
        ruling(
            "2020-01-24",
            "After an escaped spell resolves, it returns to its owner's graveyard if it's not a permanent " +
                "spell. If it is a permanent spell, it enters the battlefield and will return to its owner's " +
                "graveyard if it dies later. Perhaps it will escape again—good underworld security is so hard " +
                "to come by these days."
        )
    }
}
