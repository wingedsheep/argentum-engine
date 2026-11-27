package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.DynamicAmounts

/**
 * Demonic Rising
 * {3}{B}{B}
 * Enchantment
 *
 * At the beginning of your end step, if you control exactly one creature, create a 5/5 black
 * Demon creature token with flying.
 *
 * The intervening "if" is an equality [Conditions.CompareAmounts] over the count of creatures you
 * control, so it is checked both when the end step begins and again on resolution.
 */
val DemonicRising = card("Demonic Rising") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your end step, if you control exactly one creature, create a " +
        "5/5 black Demon creature token with flying."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).count(),
            ComparisonOperator.EQ,
            1
        )
        effect = Effects.CreateToken(
            power = 5,
            toughness = 5,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Demon"),
            keywords = setOf(Keyword.FLYING)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "94"
        artist = "Trevor Claxton"
        flavorText = "Which is the greater folly, summoning the demon or expecting gratitude from it?"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2136a82-b535-47f6-9eee-5b7585ac5cf1.jpg?1783940702"
    }
}
