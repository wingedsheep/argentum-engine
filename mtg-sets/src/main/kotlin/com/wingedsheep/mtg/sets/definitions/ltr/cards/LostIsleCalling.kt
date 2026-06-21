package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Lost Isle Calling
 * {1}{U}
 * Enchantment
 *
 * Whenever you scry, put a verse counter on this enchantment.
 * {4}{U}{U}, Exile this enchantment: Draw a card for each verse counter on this enchantment.
 * If it had seven or more verse counters on it, take an extra turn after this one.
 * Activate only as a sorcery.
 *
 * The activated ability exiles its own source as part of the cost, so by the time the effect
 * resolves the source — and its verse counters (CR 122.2) — are gone. The pre-cost count is
 * snapshotted into the resolution context at cost-payment time and read back via
 * [DynamicAmount.LastKnownSourceCounters] for both the draw amount and the seven-or-more test.
 */
val LostIsleCalling = card("Lost Isle Calling") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever you scry, put a verse counter on this enchantment.\n" +
        "{4}{U}{U}, Exile this enchantment: Draw a card for each verse counter on this " +
        "enchantment. If it had seven or more verse counters on it, take an extra turn after " +
        "this one. Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.WheneverYouScry
        effect = Effects.AddCounters(Counters.VERSE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{U}{U}"), Costs.ExileSelf)
        timing = TimingRule.SorcerySpeed
        effect = Effects.Composite(
            listOf(
                Effects.DrawCards(
                    DynamicAmounts.lastKnownSourceCounters(CounterTypeFilter.Named(Counters.VERSE))
                ),
                ConditionalEffect(
                    condition = Compare(
                        DynamicAmounts.lastKnownSourceCounters(CounterTypeFilter.Named(Counters.VERSE)),
                        ComparisonOperator.GTE,
                        DynamicAmount.Fixed(7)
                    ),
                    effect = Effects.TakeExtraTurn()
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Wangjie Li"
        flavorText = "\"To the Sea, to the Sea!\""
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b744230-8127-4d4f-9d40-75e7c2aab77c.jpg?1686968202"
    }
}
