package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Fresh-Faced Recruit
 * {1}{R/W}
 * Creature — Human Soldier
 * 2/1
 *
 * During your turn, this creature has first strike.
 */
val FreshFacedRecruit = card("Fresh-Faced Recruit") {
    manaCost = "{1}{R/W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 1
    oracleText = "During your turn, this creature has first strike."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FIRST_STRIKE, Filters.Self),
            condition = Conditions.IsYourTurn,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "216"
        artist = "Anthony Palumbo"
        flavorText = "\"Hold on to your ideals. They'll be tested more than your armor or the edge of your blade.\"\n—Tajic"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8b19518-7752-4673-a7d8-ae2e0070129c.jpg?1783934118"
    }
}
