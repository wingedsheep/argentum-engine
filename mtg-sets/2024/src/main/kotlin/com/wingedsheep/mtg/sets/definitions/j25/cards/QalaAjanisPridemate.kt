package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Qala, Ajani's Pridemate — Foundations Jumpstart #32
 * {3}{W} · Legendary Creature — Cat Warrior · 3/3
 *
 * Whenever Qala attacks, other attacking creatures you control get +X/+0 until end of turn, where
 * X is the number of counters on Qala.
 * Whenever you gain life, put a +1/+1 counter on Qala.
 * {3}{W}: You gain 1 life.
 *
 * X counts counters of every kind on Qala ([DynamicAmounts.countersOnSelf] with a `null` type) and
 * is read as the attack trigger resolves, then locked into a fixed +X/+0 per creature (ruling
 * 2024-11-08). If Qala has left the battlefield by then, the evaluator falls back to her
 * last-known counters.
 */
val QalaAjanisPridemate = card("Qala, Ajani's Pridemate") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Cat Warrior"
    power = 3
    toughness = 3
    oracleText = "Whenever Qala attacks, other attacking creatures you control get +X/+0 until end of turn, " +
        "where X is the number of counters on Qala.\n" +
        "Whenever you gain life, put a +1/+1 counter on Qala.\n" +
        "{3}{W}: You gain 1 life."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Group.modifyStatsForAll(
            power = DynamicAmounts.countersOnSelf(null),
            toughness = DynamicAmounts.fixed(0),
            filter = GroupFilter(GameObjectFilter.Creature.attacking().youControl(), excludeSelf = true)
        )
    }

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Mana("{3}{W}")
        effect = Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "32"
        artist = "Yoshiya"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3cf1e9ab-343d-462c-a71e-e12b101aa0b4.jpg?1783908864"
        ruling("2024-11-08", "The value of X is calculated only once, as Qala's first ability resolves.")
        ruling(
            "2024-11-08",
            "Qala's second ability triggers just once for each life-gaining event, whether it's 1 life from " +
                "Dazzling Angel or 4 life from Apothecary Stomper."
        )
        ruling(
            "2024-11-08",
            "If Qala is dealt lethal damage at the same time that you gain life, it won't receive a counter " +
                "from its second ability in time to save it."
        )
    }
}
