package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Growth Cycle
 * {1}{G}
 * Instant
 * Target creature gets +3/+3 until end of turn. It gets an additional +2/+2 until end of turn
 * for each card named Growth Cycle in your graveyard.
 *
 * The bonus is `3 + 2 × (cards named Growth Cycle in your graveyard)`, counted as the spell
 * resolves. Growth Cycle itself is still on the stack then, so it never counts itself.
 */
val GrowthCycle = card("Growth Cycle") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +3/+3 until end of turn. It gets an additional +2/+2 until " +
        "end of turn for each card named Growth Cycle in your graveyard."

    spell {
        val t = target(TargetFilter.Creature)
        val bonus = 3 + DynamicAmounts.count(
            Player.You,
            Zone.GRAVEYARD,
            GameObjectFilter(
                cardPredicates = listOf(CardPredicate.NameEquals("Growth Cycle")),
            ),
        ) * 2
        effect = Effects.ModifyStats(bonus, bonus, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "175"
        artist = "Svetlin Velinov"
        flavorText = "\"Shed your faded husk! Emerge sleeker and stronger!\"\n—Tyris, fauna shaman"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e619121-f2e8-4da5-8652-a2b5dae2e55c.jpg?1783932964"
        ruling(
            "2019-07-12",
            "The size of the bonus is determined as Growth Cycle begins to resolve; it won't change " +
                "later in the turn if the number of Growth Cycles in your graveyard changes.",
        )
        ruling(
            "2019-07-12",
            "Because Growth Cycle is still on the stack as you perform its instructions, it isn't in " +
                "your graveyard and won't add to the bonus the creature gets.",
        )
    }
}
