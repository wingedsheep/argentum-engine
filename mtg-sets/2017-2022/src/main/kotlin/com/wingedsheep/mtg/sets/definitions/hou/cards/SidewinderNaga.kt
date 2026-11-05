package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

private val desertCondition = Conditions.Any(
    Conditions.ControlPermanentOfType(Subtype.DESERT),
    Conditions.GraveyardContainsSubtype(Subtype.DESERT),
)

/**
 * Sidewinder Naga — Hour of Devastation #134 (canonical printing)
 * {2}{G} · Creature — Snake Warrior · 3/2
 *
 * As long as you control a Desert or there is a Desert card in your graveyard, this creature gets
 * +1/+0 and has trample.
 *
 * The same either-half Desert condition Solitary Camel uses, gating a layer-7c self [ModifyStats]
 * and a layer-6 self [GrantKeyword].
 */
val SidewinderNaga = card("Sidewinder Naga") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake Warrior"
    power = 3
    toughness = 2
    oracleText = "As long as you control a Desert or there is a Desert card in your graveyard, " +
        "this creature gets +1/+0 and has trample."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 0, GroupFilter.source()),
            condition = desertCondition,
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.TRAMPLE, GroupFilter.source()),
            condition = desertCondition,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Victor Adame Minguez"
        flavorText = "\"Those who embrace the harsh land rather than fight it find they have a powerful ally.\"\n—Nissa Revane"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3786d75-1a8a-4917-aa98-6836df1b9145.jpg?1783936013"
        ruling("2017-07-14", "If an ability checks whether you control a Desert or there is a Desert card in your graveyard, having more than one doesn’t matter. Controlling one is the same as controlling five. There is also no extra bonus for both controlling one and having one in your graveyard.")
    }
}
