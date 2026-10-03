package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Quest for the Necropolis
 * {B}
 * Enchantment
 *
 * Landfall — Whenever a land you control enters, put a quest counter on this enchantment.
 * {5}{B}, Sacrifice this enchantment: Put target creature card from a graveyard onto the
 * battlefield under your control. This ability costs {1} less to activate for each quest counter
 * on this enchantment. Activate only as a sorcery.
 *
 * The reduction reads the quest counters while the total cost is determined, before the sacrifice
 * is paid; it only reduces generic mana, so the {B} always remains.
 */
val QuestForTheNecropolis = card("Quest for the Necropolis") {
    manaCost = "{B}"
    typeLine = "Enchantment"
    oracleText = "Landfall — Whenever a land you control enters, put a quest counter on this enchantment.\n" +
        "{5}{B}, Sacrifice this enchantment: Put target creature card from a graveyard onto the battlefield under your control. This ability costs {1} less to activate for each quest counter on this enchantment. Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.AddCounters(CounterType.QUEST, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}{B}"), Costs.SacrificeSelf)
        val creature = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.PutOntoBattlefieldFromGraveyard(creature, underYourControl = true)
        genericCostReduction = DynamicAmounts.countersOnSelf(CounterType.QUEST)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "104"
        artist = "Jorge Jacinto"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/813020ef-9d98-4f79-a953-85e7e7e5d3e1.jpg?1783911276"
    }
}
