package com.wingedsheep.mtg.sets.definitions.avr.cards

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
 * Homicidal Seclusion
 * {4}{B}
 * Enchantment
 *
 * As long as you control exactly one creature, that creature gets +3/+1 and has lifelink.
 *
 * Same shape as Deadly Wanderings: while you control exactly one creature, "creatures you
 * control" is precisely "that creature", so the group filter names it exactly.
 */
val HomicidalSeclusion = card("Homicidal Seclusion") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "As long as you control exactly one creature, that creature gets +3/+1 and has lifelink."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CompositeStaticAbility(
                listOf(
                    ModifyStats(powerBonus = 3, toughnessBonus = 1, filter = GroupFilter.AllCreaturesYouControl),
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
        collectorNumber = "108"
        artist = "Cliff Childs"
        flavorText = "\"Damned crows and their endless squawking . . . blocks out the voices in my head . . .\"\n—Hermit of Hanne Hall"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/022960a1-8223-4b83-aea2-85359c39f3b8.jpg?1783940696"
    }
}
