package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Thassa's Intervention
 * {X}{U}{U}
 * Instant
 * Choose one —
 * • Look at the top X cards of your library. Put up to two of them into your hand and the rest on
 *   the bottom of your library in a random order.
 * • Counter target spell unless its controller pays twice {X}.
 *
 * The blue Intervention: both modes read the one X paid at cast time. The dig is
 * [Patterns.Library.lookAtTopAndTakeMatching] over any card with an "up to two" selection (rest to
 * the bottom in a random order, the recipe's default); the soft counter is Condescend's
 * [Effects.CounterUnlessDynamicPays] with the amount doubled.
 */
val ThassasIntervention = card("Thassa's Intervention") {
    manaCost = "{X}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Look at the top X cards of your library. Put up to two of them into your hand and the rest on " +
        "the bottom of your library in a random order.\n" +
        "• Counter target spell unless its controller pays twice {X}."

    spell {
        modal(chooseCount = 1) {
            mode("Look at the top X cards of your library and put up to two of them into your hand") {
                effect = Patterns.Library.lookAtTopAndTakeMatching(
                    count = DynamicAmounts.xValue(),
                    filter = GameObjectFilter.Any,
                    prompt = "Put up to two of them into your hand",
                    selection = SelectionMode.ChooseUpTo(DynamicAmounts.fixed(2)),
                )
            }
            mode("Counter target spell unless its controller pays twice {X}") {
                target(TargetFilter.SpellOnStack)
                effect = Effects.CounterUnlessDynamicPays(DynamicAmounts.xValue() * 2)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "72"
        artist = "Zack Stella"
        flavorText = "\"I bring a flood that nothing can withstand.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c1241d0-20d4-4eab-970d-74e476f023b4.jpg?1783931577"
    }
}
