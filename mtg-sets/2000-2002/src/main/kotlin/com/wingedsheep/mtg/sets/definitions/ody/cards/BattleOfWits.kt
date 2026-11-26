package com.wingedsheep.mtg.sets.definitions.ody.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Battle of Wits
 * {3}{U}{U}
 * Enchantment
 * At the beginning of your upkeep, if you have 200 or more cards in your library, you win the game.
 *
 * Test of Endurance's shape with a library-size threshold: the count is an intervening "if",
 * checked as the upkeep begins and again on resolution.
 */
val BattleOfWits = card("Battle of Wits") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, if you have 200 or more cards in your library, you win the game."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.count(Player.You, Zone.LIBRARY),
            ComparisonOperator.GTE,
            200,
        )
        effect = Effects.WinGame()
        description = "At the beginning of your upkeep, if you have 200 or more cards in your library, you win the game."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "69"
        artist = "Mark Brill"
        flavorText = "The wizard who reads a thousand books is powerful. The wizard who memorizes a thousand books is insane."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57f3c75b-0fe0-48e0-b468-bb928565e9dd.jpg?1783945265"
        ruling("2012-07-01", "Battle of Wits has an \"intervening 'if' clause.\" This means that if you don't have 200 or more cards in your library at the beginning of your upkeep, the ability won't trigger. If the ability does trigger, but you have fewer than 200 cards in your library when the ability resolves, the ability will do nothing.")
    }
}
