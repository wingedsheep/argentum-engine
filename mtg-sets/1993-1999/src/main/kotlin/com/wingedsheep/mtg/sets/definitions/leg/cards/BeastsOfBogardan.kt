package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Beasts of Bogardan
 * {4}{R}
 * Creature — Beast
 * 3/3
 * Protection from red
 * This creature gets +1/+1 as long as an opponent controls a nontoken white permanent.
 *
 * The Water Wurm shape: an `Exists` over the opponents' battlefields, re-asked on every
 * projection, here filtering for a nontoken white permanent.
 */
val BeastsOfBogardan = card("Beasts of Bogardan") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 3
    oracleText = "Protection from red\n" +
        "This creature gets +1/+1 as long as an opponent controls a nontoken white permanent."

    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.RED)))

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 1, Filters.Self),
            condition = Conditions.OpponentControls(
                GameObjectFilter.Permanent.withColor(Color.WHITE).nontoken()
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "133"
        artist = "Daniel Gelon"
        flavorText = "Bogardan is a land as volatile as the creatures who live there."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f885d776-2953-4ed4-b63f-91dc2b42783b.jpg?1783948059"
    }
}
