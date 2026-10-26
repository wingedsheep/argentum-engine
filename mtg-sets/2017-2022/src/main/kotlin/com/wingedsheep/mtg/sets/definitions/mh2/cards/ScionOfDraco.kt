package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Scion of Draco
 * {12}
 * Artifact Creature — Dragon
 * 4/4
 *
 * Domain — This spell costs {2} less to cast for each basic land type among lands you control.
 * Flying
 * Each creature you control has vigilance if it's white, hexproof if it's blue, lifelink if it's
 * black, first strike if it's red, and trample if it's green.
 *
 * Domain is Leyline Binding's self-cast generic reduction over [DynamicAmounts.domain], doubled.
 * The keyword clause is five colour-filtered static grants over creatures you control; each group
 * is re-read from projected state, so a creature that changes colour gains or loses the matching
 * keyword, and a multicoloured creature gets every keyword its colours name.
 */
val ScionOfDraco = card("Scion of Draco") {
    manaCost = "{12}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Domain — This spell costs {2} less to cast for each basic land type among lands you control.\n" +
        "Flying\n" +
        "Each creature you control has vigilance if it's white, hexproof if it's blue, lifelink if it's black, " +
        "first strike if it's red, and trample if it's green."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.domain() * 2)
            ),
        )
    }

    keywords(Keyword.FLYING)

    listOf(
        Color.WHITE to Keyword.VIGILANCE,
        Color.BLUE to Keyword.HEXPROOF,
        Color.BLACK to Keyword.LIFELINK,
        Color.RED to Keyword.FIRST_STRIKE,
        Color.GREEN to Keyword.TRAMPLE,
    ).forEach { (color, keyword) ->
        staticAbility {
            ability = GrantKeyword(
                keyword,
                filter = GroupFilter(GameObjectFilter.Creature.youControl().withColor(color))
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "234"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e7da55c-7f05-46b2-aa3c-17f8d5df46bb.jpg?1783926802"
        ruling(
            "2021-06-18",
            "Domain abilities count the number of basic land types among lands you control, not how many " +
                "lands you control or how many of any type."
        )
        ruling(
            "2021-06-18",
            "The basic land types are Plains, Island, Swamp, Mountain, and Forest. Land types other than " +
                "basic land types (such as Desert) don't contribute to domain abilities."
        )
    }
}
