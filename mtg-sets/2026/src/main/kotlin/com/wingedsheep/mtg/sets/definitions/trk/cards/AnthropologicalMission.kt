package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Anthropological Mission
 * {2}{U}
 * Enchantment
 *
 * Whenever you cast a noncreature spell, put a quest counter on this enchantment. Draw a card.
 * Then discard a card unless this enchantment has three or more quest counters on it.
 *
 * The counter count is read at resolution after the counter lands, so the third trigger already
 * skips the discard.
 */
val AnthropologicalMission = card("Anthropological Mission") {
    manaCost = "{2}{U}"
    typeLine = "Enchantment"
    colorIdentity = "U"
    oracleText = "Whenever you cast a noncreature spell, put a quest counter on this enchantment. " +
        "Draw a card. Then discard a card unless this enchantment has three or more quest counters on it."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.AddCounters(CounterType.QUEST, 1, EffectTarget.Self) then
            Effects.DrawCards(1) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.countersOnSelf(CounterType.QUEST),
                    ComparisonOperator.LT,
                    3
                ),
                then = Effects.Discard(1)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "Pablo Mendoza"
        flavorText = "While the Prime Directive doesn't allow the Federation to interfere with pre-warp " +
            "societies, it can learn from them."
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93b458aa-a928-48ef-b685-1306cc65ad06.jpg?1793012962"
    }
}
