package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Deep-Sea Terror
 * {4}{U}{U}
 * Creature — Serpent
 * 6/6
 * This creature can't attack unless there are seven or more cards in your graveyard.
 */
val DeepSeaTerror = card("Deep-Sea Terror") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Serpent"
    power = 6
    toughness = 6
    oracleText = "This creature can't attack unless there are seven or more cards in your graveyard."

    staticAbility {
        ability = CantAttackUnless(
            Conditions.CompareAmounts(
                DynamicAmounts.cardsInYourGraveyard(),
                ComparisonOperator.GTE,
                7
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Marco Nelor"
        flavorText = "After stripping the sunken ships, it rises to the surface for another helping."
        imageUri = "https://cards.scryfall.io/normal/front/0/0/007b526c-5387-4ed4-a320-b826d507e014.jpg?1783938352"
        ruling("2015-06-22", "Deep-Sea Terror's ability only checks as attackers are declared. After Deep-Sea Terror legally attacks, reducing the number of cards in your graveyard won't cause it to stop attacking.")
    }
}
