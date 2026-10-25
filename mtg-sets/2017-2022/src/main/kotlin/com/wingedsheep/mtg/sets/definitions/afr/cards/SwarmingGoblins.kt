package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Swarming Goblins
 * {4}{R}
 * Creature — Goblin
 * 4/3
 * When this creature enters, roll a d20.
 * 1—9 | Create a 1/1 red Goblin creature token.
 * 10—19 | Create two of those tokens.
 * 20 | Create three of those tokens.
 */
val SwarmingGoblins = card("Swarming Goblins") {
    manaCost = "{4}{R}"
    typeLine = "Creature — Goblin"
    power = 4
    toughness = 3
    oracleText = "When this creature enters, roll a d20.\n" +
        "1—9 | Create a 1/1 red Goblin creature token.\n" +
        "10—19 | Create two of those tokens.\n" +
        "20 | Create three of those tokens."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val result = rollDiceResult(sides = 20)
            fun goblins(count: Int) = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Goblin"),
                count = count,
                imageUri = "https://cards.scryfall.io/normal/front/1/4/1425e965-7eea-419c-a7ec-c8169fa9edbf.jpg?1783926342",
            )
            fun between(low: Int, high: Int) = Conditions.All(
                Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, low),
                Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, high),
            )
            ifThen(between(1, 9)) { run(goblins(1)) }
            ifThen(between(10, 19)) { run(goblins(2)) }
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, 20)) { run(goblins(3)) }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "162"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/623c1d1b-69a4-4bc4-b388-17a7600fd960.jpg?1783926472"
    }
}
