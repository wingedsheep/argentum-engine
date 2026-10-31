package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Garenbrig Paladin
 * {4}{G}
 * Creature — Giant Knight
 * 4/4
 * Adamant — If at least three green mana was spent to cast this spell, this creature enters with
 * a +1/+1 counter on it.
 * This creature can't be blocked by creatures with power 2 or less.
 *
 * Adamant counts green mana spent on the whole cost, generic included, so it reads
 * [DynamicAmounts.manaOfColorSpent] against the entering creature's own cast.
 */
val GarenbrigPaladin = card("Garenbrig Paladin") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Giant Knight"
    power = 4
    toughness = 4
    oracleText = "Adamant — If at least three green mana was spent to cast this spell, this creature enters with a +1/+1 counter on it.\n" +
        "This creature can't be blocked by creatures with power 2 or less."

    replacementEffect(
        EntersWithCounters(
            count = 1,
            selfOnly = true,
            condition = Conditions.CompareAmounts(
                DynamicAmounts.manaOfColorSpent(Color.GREEN),
                ComparisonOperator.GTE,
                3,
            ),
        )
    )

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.powerAtMost(2))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "157"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/8445020a-6fab-47dd-9e57-886c28f68b3f.jpg?1783932610"
    }
}
