package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.becomesDayAsEntersIfNeither
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vadrik, Astral Archmage
 * {1}{U}{R}
 * Legendary Creature — Human Wizard
 * 1/2
 * If it's neither day nor night, it becomes day as Vadrik enters.
 * Instant and sorcery spells you cast cost {X} less to cast, where X is Vadrik's power.
 * Whenever day becomes night or night becomes day, put a +1/+1 counter on Vadrik.
 *
 * The discount is Golden-Tail Trainer's shape: a battlefield [SpellCostTarget.YouCast] generic-only
 * reduction whose amount is [DynamicAmounts.sourcePower] (projected, so the counters raise it).
 */
val VadrikAstralArchmage = card("Vadrik, Astral Archmage") {
    manaCost = "{1}{U}{R}"
    typeLine = "Legendary Creature — Human Wizard"
    power = 1
    toughness = 2
    oracleText = "If it's neither day nor night, it becomes day as Vadrik enters.\n" +
        "Instant and sorcery spells you cast cost {X} less to cast, where X is Vadrik's power.\n" +
        "Whenever day becomes night or night becomes day, put a +1/+1 counter on Vadrik."

    becomesDayAsEntersIfNeither()

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.InstantOrSorcery),
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.sourcePower())
            ),
        )
    }

    triggeredAbility {
        trigger = Triggers.dayBecomesNightOrNightBecomesDay
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever day becomes night or night becomes day, put a +1/+1 counter on Vadrik."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "248"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/454666bb-f81d-4845-84aa-d6f8f80ce86a.jpg?1783925550"
    }
}
