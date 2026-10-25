package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Arcane Investigator
 * {1}{U}
 * Creature — Elf Wizard
 * 2/1
 * Search the Room — {5}{U}: Roll a d20.
 * 1—9 | Draw a card.
 * 10—20 | Look at the top three cards of your library. Put one of them into your hand and the
 * rest on the bottom of your library in any order.
 */
val ArcaneInvestigator = card("Arcane Investigator") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Elf Wizard"
    power = 2
    toughness = 1
    oracleText = "Search the Room — {5}{U}: Roll a d20.\n" +
        "1—9 | Draw a card.\n" +
        "10—20 | Look at the top three cards of your library. Put one of them into your hand and the rest on the bottom of your library in any order."

    activatedAbility {
        cost = Costs.Mana("{5}{U}")
        effect = Effects.Pipeline {
            val result = rollDiceResult(sides = 20)
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, 9)) {
                run(Effects.DrawCards(1))
            }
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, 10)) {
                run(
                    Patterns.Library.lookAtTopAndKeep(
                        count = 3,
                        keepCount = 1,
                        keepDestination = CardDestination.ToZone(Zone.HAND),
                        restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                        restOrder = CardOrder.ControllerChooses,
                    )
                )
            }
        }
        description = "Search the Room — {5}{U}: Roll a d20. 1—9: draw a card. 10—20: look at the top three " +
            "cards of your library, put one into your hand and the rest on the bottom in any order."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Bram Sels"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc457520-9947-4f65-bbe7-9b95bd2c23af.jpg?1783926518"
        ruling("2021-07-23", "The instruction to roll a die and the effect that occurs because of the result are all part of the same ability. Players do not get the chance to respond to the ability after knowing the result of the roll.")
    }
}
