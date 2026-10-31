package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Primal Empathy — Ikoria: Lair of Behemoths #200
 * {1}{G}{U} · Enchantment
 *
 * At the beginning of your upkeep, draw a card if you control a creature with the greatest power
 * among creatures on the battlefield. Otherwise, put a +1/+1 counter on a creature you control.
 *
 * No intervening-if: the trigger always goes on the stack and the power comparison is made as it
 * resolves. "You control a creature with the greatest power" ≡ you control a creature AND your
 * highest creature power is at least the battlefield-wide highest (ties count). The counter's
 * creature is chosen on resolution and isn't targeted.
 */
val PrimalEmpathy = card("Primal Empathy") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, draw a card if you control a creature with the " +
        "greatest power among creatures on the battlefield. Otherwise, put a +1/+1 counter on a " +
        "creature you control."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.If(
            Conditions.All(
                Conditions.ControlCreature,
                Conditions.CompareAmounts(
                    DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower(),
                    ComparisonOperator.GTE,
                    DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Creature).maxPower()
                )
            ),
            then = Effects.DrawCards(1),
            otherwise = Effects.Pipeline {
                val chosen = selectTarget(
                    TargetObject(filter = TargetFilter.CreatureYouControl),
                    nonTargeting = true,
                    prompt = "Choose a creature you control to get a +1/+1 counter"
                )
                run(Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, chosen.asTarget))
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "200"
        artist = "Micah Epstein"
        flavorText = "\"From this day forward, we confront this world together, lifting each other up.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/3/034e0afb-7787-4cb8-8bb4-cf6997dbf7e4.jpg?1783931019"
    }
}
