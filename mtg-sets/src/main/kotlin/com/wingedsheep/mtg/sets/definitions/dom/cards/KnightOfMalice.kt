package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Knight of Malice
 * {1}{B}
 * Creature — Human Knight
 * 2/2
 * First strike
 * Hexproof from white (This creature can't be the target of white spells or abilities
 * your opponents control.)
 * Knight of Malice gets +1/+0 as long as any player controls a white permanent.
 */
val KnightOfMalice = card("Knight of Malice") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Human Knight"
    oracleText = "First strike\nHexproof from white (This creature can't be the target of white spells or abilities your opponents control.)\nKnight of Malice gets +1/+0 as long as any player controls a white permanent."
    power = 2
    toughness = 2

    keywords(Keyword.FIRST_STRIKE)
    keywordAbility(KeywordAbility.hexproofFrom(Color.WHITE))

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 0, GroupFilter.source()),
            condition = Exists(
                player = Player.Each,
                zone = Zone.BATTLEFIELD,
                filter = GameObjectFilter.Any.withColor(Color.WHITE)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Sidharth Chaturvedi"
        flavorText = "\"A shadow to swallow the light.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b45266f0-eb4f-4a06-bc64-8c2d774b4cc5.jpg?1562741546"
    }
}
