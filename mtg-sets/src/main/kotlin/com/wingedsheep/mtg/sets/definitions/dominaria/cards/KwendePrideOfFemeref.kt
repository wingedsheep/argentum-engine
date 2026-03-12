package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Kwende, Pride of Femeref
 * {3}{W}
 * Legendary Creature — Human Knight
 * 2/2
 * Double strike
 * Creatures you control with first strike have double strike.
 */
val KwendePrideOfFemeref = card("Kwende, Pride of Femeref") {
    manaCost = "{3}{W}"
    typeLine = "Legendary Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "Double strike\nCreatures you control with first strike have double strike."

    keywords(Keyword.DOUBLE_STRIKE)

    staticAbility {
        ability = GrantKeywordToCreatureGroup(
            keyword = Keyword.DOUBLE_STRIKE,
            filter = GroupFilter(
                GameObjectFilter.Creature.youControl().withKeyword(Keyword.FIRST_STRIKE)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Daarken"
        flavorText = "\"Descendant of a vanished land, student of a forgotten general, and master of blades no smith could forge anew. He honors them all by living a life worthy of song.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4d3b6a8-3e4a-4e2b-900a-9a21fa0ced4c.jpg?1591605307"
    }
}
