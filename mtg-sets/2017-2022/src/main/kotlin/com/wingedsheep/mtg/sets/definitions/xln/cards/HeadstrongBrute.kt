package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

val HeadstrongBrute = card("Headstrong Brute") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Orc Pirate"
    power = 3
    toughness = 3
    oracleText = "This creature can't block.\nThis creature has menace as long as you control another Pirate."

    staticAbility {
        ability = CantBlock()
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.MENACE.name, GroupFilter.source()),
            condition = Conditions.YouControl(
                GameObjectFilter.Permanent.withSubtype("Pirate"),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "147"
        artist = "Yongjae Choi"
        flavorText = "\"Watch your backs, keep your blades at hand, and let the orc go first!\"\n—Captain Brandis Thorn"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66973e90-0c8b-4438-bc22-6e78930c194d.jpg?1783935744"
    }
}
