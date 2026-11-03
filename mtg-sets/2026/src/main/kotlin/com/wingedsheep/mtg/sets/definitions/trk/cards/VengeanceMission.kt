package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vengeance Mission
 * {2}{B}
 * Enchantment
 *
 * Whenever a creature dies, put a quest counter on this enchantment. Target opponent loses 1 life.
 * That player loses 2 life instead if there are seven or more quest counters on this enchantment.
 *
 * The life-loss amount is read after the counter lands, so the seventh trigger already drains two.
 */
val VengeanceMission = card("Vengeance Mission") {
    manaCost = "{2}{B}"
    typeLine = "Enchantment"
    colorIdentity = "B"
    oracleText = "Whenever a creature dies, put a quest counter on this enchantment. Target opponent " +
        "loses 1 life. That player loses 2 life instead if there are seven or more quest counters on " +
        "this enchantment."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).dies()
        val opponent = target(Targets.Opponent)
        effect = Effects.AddCounters(CounterType.QUEST, 1, EffectTarget.Self) then
            Effects.LoseLife(
                DynamicAmounts.conditional(
                    Conditions.CompareAmounts(
                        DynamicAmounts.countersOnSelf(CounterType.QUEST),
                        ComparisonOperator.GTE,
                        7
                    ),
                    2,
                    1
                ),
                opponent
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "Daarken"
        flavorText = "\"When I take this ship, I'll have Kirk's head stuffed and hung on his cabin wall.\"\n" +
            "—Commander Kang"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/059e3331-43bc-43a8-90e9-86fef37e032a.jpg?1793187091"
    }
}
