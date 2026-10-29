package com.wingedsheep.mtg.sets.definitions.jud.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Test of Endurance
 * {2}{W}{W}
 * Enchantment
 * At the beginning of your upkeep, if you have 50 or more life, you win the game.
 *
 * Felidar Sovereign's shape with a 50-life threshold: the life check is an intervening "if",
 * checked as the upkeep begins and again on resolution.
 */
val TestOfEndurance = card("Test of Endurance") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, if you have 50 or more life, you win the game."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.LifeAtLeast(50)
        effect = Effects.WinGame()
        description = "At the beginning of your upkeep, if you have 50 or more life, you win the game."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "29"
        artist = "Mike Ploog"
        flavorText = "\"If we have learned nothing else, we have learned to survive.\"\n—Mystic elder"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf16bd6b-e99c-4da3-bd03-11f63b7ee85d.jpg?1783945131"
        ruling("2022-12-08", "Test of Endurance's ability will check your life total as your upkeep begins, before any player has the opportunity to take any actions. If you don't have 50 or more life at that time, the ability won't trigger at all. If the ability does trigger, it will check again as it tries to resolve. If your life total has fallen below 50 at that point, the ability won't have any effect.")
    }
}
