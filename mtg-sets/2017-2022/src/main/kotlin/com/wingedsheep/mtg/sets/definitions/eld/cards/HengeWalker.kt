package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Henge Walker
 * {3}
 * Artifact Creature — Golem
 * 2/2
 * Adamant — If at least three mana of the same color was spent to cast this spell, this creature
 * enters with a +1/+1 counter on it.
 *
 * "The same color" is any one of the five colors, so the condition is the OR of three-or-more of
 * each color spent on the (all-generic) cost. Not cast, or cast without paying mana: no counter.
 */
private val threeOfOneColorSpent = Conditions.Any(
    *Color.entries.map { color ->
        Conditions.CompareAmounts(DynamicAmounts.manaOfColorSpent(color), ComparisonOperator.GTE, 3)
    }.toTypedArray()
)

val HengeWalker = card("Henge Walker") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Golem"
    power = 2
    toughness = 2
    oracleText = "Adamant — If at least three mana of the same color was spent to cast this spell, this creature enters with a +1/+1 counter on it."

    replacementEffect(
        EntersWithCounters(
            count = 1,
            selfOnly = true,
            condition = threeOfOneColorSpent,
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "221"
        artist = "Titus Lunter"
        flavorText = "Everything that inhabits Garenbrig is affected by the wild magic flowing through the Great Henge."
        imageUri = "https://cards.scryfall.io/normal/front/9/9/9925f0bc-8685-4e38-8864-0ad3a9549ab5.jpg?1783932586"
        ruling("2019-10-04", "Adamant effects check what mana was actually spent to cast a spell. If an effect allows you to spend mana \"as though it were mana\" of any color or type, that allows you to spend mana you couldn't otherwise spend, but it doesn't change what mana you spent to cast the spell.")
        ruling("2019-10-04", "If you copy a spell that has an adamant ability, no mana was spent to cast the copy at all, so that ability won't apply.")
    }
}
