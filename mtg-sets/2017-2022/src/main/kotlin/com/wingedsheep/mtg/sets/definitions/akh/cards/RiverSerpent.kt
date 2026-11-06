package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * River Serpent
 * {5}{U}
 * Creature — Serpent
 * 5/5
 * This creature can't attack unless there are five or more cards in your graveyard.
 * Cycling {U} ({U}, Discard this card: Draw a card.)
 */
val RiverSerpent = card("River Serpent") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Serpent"
    power = 5
    toughness = 5
    oracleText = "This creature can't attack unless there are five or more cards in your graveyard.\n" +
        "Cycling {U} ({U}, Discard this card: Draw a card.)"

    staticAbility {
        ability = CantAttackUnless(
            Conditions.CompareAmounts(
                DynamicAmounts.cardsInYourGraveyard(),
                ComparisonOperator.GTE,
                5
            )
        )
    }

    keywordAbility(KeywordAbility.cycling("{U}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Christopher Moeller"
        flavorText = "Though the Luxa River brings life to Naktamun, it's not without dangers."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a89500ba-8256-4002-a9f2-62d529e9886b.jpg?1783936516"
        ruling("2017-04-18", "Once River Serpent has attacked, it will remain in combat even if the number of cards in your graveyard becomes four or fewer.")
    }
}
