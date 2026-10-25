package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Earth-Cult Elemental
 * {4}{R}{R}
 * Creature — Elemental
 * 6/6
 * Siege Monster — When this creature enters, roll a d20.
 * 1—9 | Each player sacrifices a permanent of their choice.
 * 10—19 | Each opponent sacrifices a permanent of their choice.
 * 20 | Each opponent sacrifices two permanents of their choice.
 */
val EarthCultElemental = card("Earth-Cult Elemental") {
    manaCost = "{4}{R}{R}"
    typeLine = "Creature — Elemental"
    power = 6
    toughness = 6
    oracleText = "Siege Monster — When this creature enters, roll a d20.\n" +
        "1—9 | Each player sacrifices a permanent of their choice.\n" +
        "10—19 | Each opponent sacrifices a permanent of their choice.\n" +
        "20 | Each opponent sacrifices two permanents of their choice."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val result = rollDiceResult(sides = 20)
            fun between(low: Int, high: Int) = Conditions.All(
                Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, low),
                Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, high),
            )
            ifThen(between(1, 9)) {
                run(Effects.Sacrifice(GameObjectFilter.Permanent, 1, EffectTarget.PlayerRef(Player.Each)))
            }
            ifThen(between(10, 19)) {
                run(Effects.Sacrifice(GameObjectFilter.Permanent, 1, EffectTarget.PlayerRef(Player.EachOpponent)))
            }
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, 20)) {
                run(Effects.Sacrifice(GameObjectFilter.Permanent, 2, EffectTarget.PlayerRef(Player.EachOpponent)))
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "141"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa05f317-b2f8-4eea-951b-0c374774764b.jpg?1783926480"
        ruling("2021-07-23", "The instruction to roll a die and the effect that occurs because of the result are all part of the same ability. Players do not get the chance to respond to the ability after knowing the result of the roll.")
    }
}
