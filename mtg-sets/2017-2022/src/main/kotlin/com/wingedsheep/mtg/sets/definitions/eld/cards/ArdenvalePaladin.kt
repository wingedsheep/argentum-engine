package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Ardenvale Paladin
 * {3}{W}
 * Creature — Human Knight
 * 2/5
 * Adamant — If at least three white mana was spent to cast this spell, this creature enters with
 * a +1/+1 counter on it.
 *
 * Adamant counts white mana spent on the whole cost, generic included, so it reads
 * [DynamicAmounts.manaOfColorSpent] against the entering creature's own cast. A Paladin put onto
 * the battlefield without being cast spent no mana and gets no counter.
 */
val ArdenvalePaladin = card("Ardenvale Paladin") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 5
    oracleText = "Adamant — If at least three white mana was spent to cast this spell, this creature enters with a +1/+1 counter on it."

    replacementEffect(
        EntersWithCounters(
            count = 1,
            selfOnly = true,
            condition = Conditions.CompareAmounts(
                DynamicAmounts.manaOfColorSpent(Color.WHITE),
                ComparisonOperator.GTE,
                3,
            ),
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "4"
        artist = "Volkan Baǵa"
        flavorText = "\"Even when hope is but a small flame fighting the night, I will serve the absent king.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a32fa360-6c41-4146-931b-c19e9a766803.jpg?1783932679"
    }
}
