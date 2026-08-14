package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Armory Mice
 * {1}{W}
 * Creature — Mouse
 * 3/1
 *
 * Celebration — This creature gets +0/+2 as long as two or more nonland permanents entered the
 * battlefield under your control this turn.
 *
 * The static half of the Celebration ability word (CR 207.2c — italic flavor, no rules meaning):
 * a [ConditionalStaticAbility] gated on [Conditions.Celebration], so the bonus is re-evaluated
 * every projection and appears the instant the second nonland permanent enters.
 */
val ArmoryMice = card("Armory Mice") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Mouse"
    power = 3
    toughness = 1
    oracleText = "Celebration — This creature gets +0/+2 as long as two or more nonland " +
        "permanents entered the battlefield under your control this turn."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 0, toughnessBonus = 2, filter = GroupFilter.source()),
            condition = Conditions.Celebration,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Chris Seaman"
        flavorText = "They help ensure that the forges of the dwarves are always squeaky clean."
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b041949-6fb6-40a6-9329-f209be537219.jpg?1783915136"
    }
}
