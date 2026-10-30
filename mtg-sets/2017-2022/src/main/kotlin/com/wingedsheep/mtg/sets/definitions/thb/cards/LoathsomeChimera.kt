package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Loathsome Chimera
 * {2}{G}
 * Creature — Chimera
 * 4/1
 * Escape—{4}{G}, Exile three other cards from your graveyard.
 * This creature escapes with a +1/+1 counter on it.
 *
 * Ox of Agonas's shape: [KeywordAbility.escape] plus an [EntersWithCounters] replacement gated on
 * [Conditions.Escaped] (CR 702.138c).
 */
val LoathsomeChimera = card("Loathsome Chimera") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Chimera"
    power = 4
    toughness = 1
    oracleText = "Escape—{4}{G}, Exile three other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)\n" +
        "This creature escapes with a +1/+1 counter on it."

    keywordAbility(KeywordAbility.escape("{4}{G}", Costs.additional.ExileOtherCards(3)))

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 1,
        selfOnly = true,
        condition = Conditions.Escaped
    ))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "177"
        artist = "Paul Scott Canavan"
        flavorText = "\"I see... elk? And lion? And... teeth! A lot of teeth!\"\n—Teraklos of Meletis"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3afe6283-3642-4883-ac6e-83ae3280af9c.jpg?1783931537"
        ruling(
            "2020-01-24",
            "After an escaped spell resolves, it returns to its owner's graveyard if it's not a permanent " +
                "spell. If it is a permanent spell, it enters the battlefield and will return to its owner's " +
                "graveyard if it dies later. Perhaps it will escape again—good underworld security is so hard " +
                "to come by these days."
        )
        ruling(
            "2020-01-24",
            "If you cast a spell with its escape permission, you can't choose to apply any other alternative " +
                "costs or to cast it without paying its mana cost. If it has any additional costs, you must pay those."
        )
    }
}
