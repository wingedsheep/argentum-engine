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
 * Locthwain Paladin
 * {3}{B}
 * Creature — Human Knight
 * 3/2
 * Menace
 * Adamant — If at least three black mana was spent to cast this spell, this creature enters with
 * a +1/+1 counter on it.
 *
 * Adamant counts black mana spent on the whole cost, generic included, so it reads
 * [DynamicAmounts.manaOfColorSpent] against the entering creature's own cast.
 */
val LocthwainPaladin = card("Locthwain Paladin") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Knight"
    power = 3
    toughness = 2
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Adamant — If at least three black mana was spent to cast this spell, this creature enters with a +1/+1 counter on it."

    keywords(Keyword.MENACE)

    replacementEffect(
        EntersWithCounters(
            count = 1,
            selfOnly = true,
            condition = Conditions.CompareAmounts(
                DynamicAmounts.manaOfColorSpent(Color.BLACK),
                ComparisonOperator.GTE,
                3,
            ),
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Zack Stella"
        flavorText = "\"I will find the king if I have to ride from summer to winter and back again.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da6f21a8-3dd0-42af-8a93-84f98968c781.jpg?1783932637"
    }
}
