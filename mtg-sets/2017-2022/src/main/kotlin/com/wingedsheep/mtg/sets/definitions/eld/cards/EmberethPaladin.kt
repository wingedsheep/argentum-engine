package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Embereth Paladin
 * {3}{R}
 * Creature — Human Knight
 * 4/1
 * Haste
 * Adamant — If at least three red mana was spent to cast this spell, this creature enters with
 * a +1/+1 counter on it.
 *
 * Adamant counts red mana spent on the whole cost, generic included, so it reads
 * [DynamicAmounts.manaOfColorSpent] against the entering creature's own cast.
 */
val EmberethPaladin = card("Embereth Paladin") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Knight"
    power = 4
    toughness = 1
    oracleText = "Haste\n" +
        "Adamant — If at least three red mana was spent to cast this spell, this creature enters with a +1/+1 counter on it."

    keywords(Keyword.HASTE)

    replacementEffect(
        EntersWithCounters(
            count = 1,
            selfOnly = true,
            condition = Conditions.CompareAmounts(
                DynamicAmounts.manaOfColorSpent(Color.RED),
                ComparisonOperator.GTE,
                3,
            ),
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "121"
        artist = "Randy Vargas"
        flavorText = "\"The king would die for the realm. I would not hesitate to do the same for him.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6bcfe06b-3bb5-434e-acf4-4bfa8a94141d.jpg?1783932626"
    }
}
