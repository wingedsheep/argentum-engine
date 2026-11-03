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
 * Support Mission
 * {2}{G}
 * Enchantment
 *
 * Whenever a creature you control enters, put a quest counter on this enchantment. Put a +1/+1
 * counter on that creature. If this enchantment has five or more quest counters on it, instead put
 * two +1/+1 counters on that creature.
 *
 * The +1/+1 count is read after the quest counter lands, so the fifth trigger already gives two.
 */
val SupportMission = card("Support Mission") {
    manaCost = "{2}{G}"
    typeLine = "Enchantment"
    colorIdentity = "G"
    oracleText = "Whenever a creature you control enters, put a quest counter on this enchantment. " +
        "Put a +1/+1 counter on that creature. If this enchantment has five or more quest counters on it, " +
        "instead put two +1/+1 counters on that creature."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.AddCounters(CounterType.QUEST, 1, EffectTarget.Self) then
            Effects.AddDynamicCounters(
                CounterType.PLUS_ONE_PLUS_ONE,
                DynamicAmounts.conditional(
                    Conditions.CompareAmounts(
                        DynamicAmounts.countersOnSelf(CounterType.QUEST),
                        ComparisonOperator.GTE,
                        5
                    ),
                    2,
                    1
                ),
                EffectTarget.TriggeringEntity
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "216"
        artist = "Nadia Hurianova"
        flavorText = "Before the Cardassian occupation, Bajor was a center of culture and learning."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e8b404c-6e2a-4966-ac4b-c9981c1c6fee.jpg?1785981605"
    }
}
