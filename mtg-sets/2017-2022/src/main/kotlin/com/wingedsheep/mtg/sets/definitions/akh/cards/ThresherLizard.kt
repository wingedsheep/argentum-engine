package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Thresher Lizard — Amonkhet #150
 * {2}{R} · Creature — Lizard · 3/2
 *
 * This creature gets +1/+2 as long as you have one or fewer cards in hand.
 */
val ThresherLizard = card("Thresher Lizard") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard"
    power = 3
    toughness = 2
    oracleText = "This creature gets +1/+2 as long as you have one or fewer cards in hand."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 1, toughnessBonus = 2, filter = GroupFilter.source()),
            condition = Conditions.CompareAmounts(
                DynamicAmounts.cardsInYourHand(),
                ComparisonOperator.LTE,
                1
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Craig J Spearing"
        flavorText = "The sound of its plated tail beating on desert rocks is often mistaken for the footfalls of a much larger predator."
        imageUri = "https://cards.scryfall.io/normal/front/9/6/9642c5f4-4229-47c4-ac44-7feb5ee9cf9e.jpg?1783936482"
        ruling("2018-03-16", "Because damage remains marked on a creature until it's removed as the turn ends, nonlethal damage dealt to Thresher Lizard while you have one or fewer cards in hand may become lethal if cards are put into your hand during that turn.")
    }
}
