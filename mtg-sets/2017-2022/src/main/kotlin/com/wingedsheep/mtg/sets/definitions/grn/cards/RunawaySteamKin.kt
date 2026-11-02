package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Runaway Steam-Kin
 * {1}{R}
 * Creature — Elemental
 * 1/1
 * Whenever you cast a red spell, if this creature has fewer than three +1/+1 counters on it, put a
 * +1/+1 counter on this creature.
 * Remove three +1/+1 counters from this creature: Add {R}{R}{R}.
 *
 * The "fewer than three" clause is an intervening if: checked when the spell is cast and again on
 * resolution (CR 603.4), so a fourth red spell while it already has three counters does nothing.
 */
val RunawaySteamKin = card("Runaway Steam-Kin") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    power = 1
    toughness = 1
    oracleText = "Whenever you cast a red spell, if this creature has fewer than three +1/+1 counters " +
        "on it, put a +1/+1 counter on this creature.\n" +
        "Remove three +1/+1 counters from this creature: Add {R}{R}{R}."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withColor(Color.RED))
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.countersOnSelf(CounterType.PLUS_ONE_PLUS_ONE),
            ComparisonOperator.LT,
            3
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.PLUS_ONE_PLUS_ONE, 3)
        effect = Effects.AddMana(Color.RED, 3)
        manaAbility = true
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "115"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8c9c111-fbc7-44e1-94bd-1ca164370623.jpg?1783934158"
        ruling("2018-10-05", "Runaway Steam-Kin has to be on the battlefield for its ability to trigger. Casting it won't trigger its own ability.")
        ruling("2018-10-05", "Runaway Steam-Kin's triggered ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2018-10-05", "If Runaway Steam-Kin has three or more +1/+1 counters on it after you're done paying for a red spell, its ability doesn't trigger at all. If it has three or more +1/+1 counters on it as its triggered ability resolves, it doesn't get a +1/+1 counter.")
    }
}
