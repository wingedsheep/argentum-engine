package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Radiant Destiny
 * {2}{W}
 * Enchantment
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)
 * As this enchantment enters, choose a creature type.
 * Creatures you control of the chosen type get +1/+1. As long as you have the city's blessing, they
 * also have vigilance.
 *
 * Ascend on a permanent is a static ability (CR 702.131b), so the keyword is the whole
 * implementation. The chosen-type anthem is the Rally the Ranks shape; the vigilance half is a
 * [ConditionalStaticAbility] gated on the city's blessing, as on Tendershoot Dryad.
 */
val RadiantDestiny = card("Radiant Destiny") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "As this enchantment enters, choose a creature type.\n" +
        "Creatures you control of the chosen type get +1/+1. As long as you have the city's blessing, " +
        "they also have vigilance."

    keywords(Keyword.ASCEND)

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter.ChosenSubtypeCreatures().youControl()
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.ChosenSubtypeCreatures().youControl()),
            condition = Conditions.YouHaveCitysBlessing
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Emrah Elmasli"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d44584d-e501-40aa-b3f2-a6155e2ee6e7.jpg?1783935337"
    }
}
