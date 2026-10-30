package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Voracious Typhon
 * {2}{G}{G}
 * Creature — Snake Beast
 * 4/4
 * Escape—{5}{G}{G}, Exile four other cards from your graveyard.
 * This creature escapes with three +1/+1 counters on it.
 *
 * Chainweb Aracnir's escape shape: [KeywordAbility.escape] plus an [EntersWithCounters]
 * replacement gated on [Conditions.Escaped].
 */
val VoraciousTyphon = card("Voracious Typhon") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake Beast"
    power = 4
    toughness = 4
    oracleText = "Escape—{5}{G}{G}, Exile four other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)\n" +
        "This creature escapes with three +1/+1 counters on it."

    keywordAbility(KeywordAbility.escape("{5}{G}{G}", Costs.additional.ExileOtherCards(4)))

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 3,
        selfOnly = true,
        condition = Conditions.Escaped
    ))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "203"
        artist = "Yongjae Choi"
        flavorText = "\"A typhon's escape from the Underworld makes armies flee and kingdoms fall.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/f/efa2bccb-0e01-4629-b9a8-5c0ea26239b3.jpg?1783931527"
        ruling(
            "2020-01-24",
            "If you cast a spell with its escape permission, you can't choose to apply any other alternative " +
                "costs or to cast it without paying its mana cost. If it has any additional costs, you must pay those."
        )
    }
}
