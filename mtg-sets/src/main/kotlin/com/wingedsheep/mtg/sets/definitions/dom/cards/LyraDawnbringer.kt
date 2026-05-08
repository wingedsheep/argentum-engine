package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Lyra Dawnbringer
 * {3}{W}{W}
 * Legendary Creature — Angel
 * 5/5
 * Flying, first strike, lifelink
 * Other Angels you control get +1/+1 and have lifelink.
 */
val LyraDawnbringer = card("Lyra Dawnbringer") {
    manaCost = "{3}{W}{W}"
    typeLine = "Legendary Creature — Angel"
    power = 5
    toughness = 5
    oracleText = "Flying, first strike, lifelink\nOther Angels you control get +1/+1 and have lifelink."

    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE, Keyword.LIFELINK)

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.youControl().withSubtype("Angel"), excludeSelf = true)
        )
    }

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.LIFELINK,
            filter = GroupFilter(GameObjectFilter.Creature.youControl().withSubtype("Angel"), excludeSelf = true)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "26"
        artist = "Chris Rahn"
        flavorText = "\"You are not alone. You never were.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93be6799-7b9d-44d4-84dc-2961692b5a85.jpg?1562739679"
        ruling("2022-12-08", "Multiple instances of lifelink on the same creature are redundant.")
        ruling("2022-12-08", "Because damage remains marked on a creature until it's removed as the turn ends, nonlethal damage dealt to an Angel you control may become lethal if Lyra leaves the battlefield during that turn.")
    }
}
