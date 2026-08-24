package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Tidal Influence
 * {2}{U}
 * Enchantment
 * Cast this spell only if no permanents named Tidal Influence are on the battlefield.
 * This enchantment enters with a tide counter on it.
 * At the beginning of your upkeep, put a tide counter on this enchantment.
 * As long as there is exactly one tide counter on this enchantment, all blue creatures get -2/-0.
 * As long as there are exactly three tide counters on this enchantment, all blue creatures get +2/+0.
 * Whenever there are four or more tide counters on this enchantment, remove all tide counters from it.
 *
 * [Homarid]'s tide, applied to the whole board instead of one creature — and symmetrically: "all
 * blue creatures" includes the opponent's. The cast restriction is global, not per-player, so a
 * second copy is uncastable while anyone controls one.
 */
val TidalInfluence = card("Tidal Influence") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Cast this spell only if no permanents named Tidal Influence are on the battlefield.\n" +
        "This enchantment enters with a tide counter on it.\n" +
        "At the beginning of your upkeep, put a tide counter on this enchantment.\n" +
        "As long as there is exactly one tide counter on this enchantment, all blue creatures get -2/-0.\n" +
        "As long as there are exactly three tide counters on this enchantment, all blue creatures get +2/+0.\n" +
        "Whenever there are four or more tide counters on this enchantment, remove all tide counters from it."

    spell {
        // Global, not per-player: a second copy is uncastable while *anyone* controls one.
        castOnlyIf(
            Exists(
                Player.Each,
                Zone.BATTLEFIELD,
                GameObjectFilter.Permanent.named("Tidal Influence"),
                negate = true,
            )
        )
    }

    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.Named(Counters.TIDE),
            count = 1,
            selfOnly = true
        )
    )

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.AddCounters(Counters.TIDE, 1, EffectTarget.Self)
        description = "At the beginning of your upkeep, put a tide counter on this enchantment."
    }

    staticAbility {
        condition = Conditions.CompareAmounts(
            DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(Counters.TIDE)),
            ComparisonOperator.EQ,
            DynamicAmount.Fixed(1),
        )
        ability = ModifyStats(-2, 0, GroupFilter(GameObjectFilter.Creature.withColor(Color.BLUE)))
    }

    staticAbility {
        condition = Conditions.CompareAmounts(
            DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(Counters.TIDE)),
            ComparisonOperator.EQ,
            DynamicAmount.Fixed(3),
        )
        ability = ModifyStats(2, 0, GroupFilter(GameObjectFilter.Creature.withColor(Color.BLUE)))
    }

    stateTriggeredAbility {
        condition = Conditions.SourceCounterCountAtLeast(Counters.TIDE, 4)
        effect = Effects.RemoveAllCountersOfType(Counters.TIDE, EffectTarget.Self)
        description = "Whenever there are four or more tide counters on this enchantment, remove all tide counters from it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "28"
        artist = "Tom Wänerstrand"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2192c7b-ef6f-4ff6-9017-b1a125340517.jpg?1783947908"
    }
}
