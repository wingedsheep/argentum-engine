package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Deadly Wanderings
 * {3}{B}{B}
 * Enchantment
 *
 * As long as you control exactly one creature, that creature gets +2/+0 and has deathtouch and lifelink.
 *
 * One printed static ability spanning two layers (keywords in 6, P/T in 7c), so it is a
 * [CompositeStaticAbility] gated by a [ConditionalStaticAbility]. While the condition holds the
 * group "creatures you control" is exactly the one creature the text calls "that creature", so
 * [GroupFilter.AllCreaturesYouControl] names it precisely. The count is re-read on every
 * projection pass, so a second creature arriving switches the whole bonus off and its leaving
 * switches it back on.
 */
val DeadlyWanderings = card("Deadly Wanderings") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "As long as you control exactly one creature, that creature gets +2/+0 and has deathtouch and lifelink."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CompositeStaticAbility(
                listOf(
                    ModifyStats(powerBonus = 2, toughnessBonus = 0, filter = GroupFilter.AllCreaturesYouControl),
                    GrantKeyword(Keyword.DEATHTOUCH, GroupFilter.AllCreaturesYouControl),
                    GrantKeyword(Keyword.LIFELINK, GroupFilter.AllCreaturesYouControl)
                )
            ),
            condition = Conditions.CompareAmounts(
                DynamicAmounts.creaturesYouControl(),
                ComparisonOperator.EQ,
                1
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        artist = "David Palumbo"
        flavorText = "\"Seclusion is only an option for the strong.\"\n—Sorin Markov"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23bb5d9f-9b12-482a-91b4-6786e8310805.jpg?1783938599"
    }
}
