package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Resolute Archangel
 * {5}{W}{W}
 * Creature — Angel
 * 4/4
 * Flying
 * When this creature enters, if your life total is less than your starting life total, it becomes
 * equal to your starting life total.
 *
 * The starting life total comes from the game's format (20, 30 in Two-Headed Giant, 40 in
 * Commander), never a hard-coded 20. Setting a life total upward is a life gain (CR 119.5), so
 * life-gain replacements and triggers apply.
 */
val ResoluteArchangel = card("Resolute Archangel") {
    manaCost = "{5}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "When this creature enters, if your life total is less than your starting life total, " +
        "it becomes equal to your starting life total."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.yourLifeTotal(),
            ComparisonOperator.LT,
            DynamicAmounts.startingLifeTotal(),
        )
        effect = Effects.SetLifeTotal(DynamicAmounts.startingLifeTotal())
        description = "When this creature enters, if your life total is less than your starting " +
            "life total, it becomes equal to your starting life total."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Anthony Palumbo"
        flavorText = "Cut it down, bury it in snow, put it to the torch. The rose will still bloom again."
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e37b31b8-d868-4dff-9ab0-723ce41ee7e4.jpg?1783939198"
        ruling(
            "2014-07-18",
            "Your starting life total is the life total you began the game with. For most two-player " +
                "formats, this is 20. For Two-Headed Giant, it's the life total your team started with, " +
                "usually 30. In Commander games, your starting life total is 40.",
        )
        ruling(
            "2014-07-18",
            "If your life total is less than your starting life total, you actually gain the appropriate " +
                "amount of life. Other effects that interact with life gain will interact with Resolute " +
                "Archangel's ability accordingly.",
        )
    }
}
