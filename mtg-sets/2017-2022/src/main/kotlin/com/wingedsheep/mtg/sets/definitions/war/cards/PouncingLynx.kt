package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Pouncing Lynx
 * {1}{W}
 * Creature — Cat, 2/1
 *
 * During your turn, this creature has first strike.
 *
 * A time-restricted static keyword grant to self — [ConditionalStaticAbility] over
 * [GrantKeyword] on [Filters.Self], gated by [Conditions.IsYourTurn] (same shape as
 * Spider-Girl, Legacy Hero's "during your turn, flying").
 */
val PouncingLynx = card("Pouncing Lynx") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat"
    oracleText = "During your turn, this creature has first strike."
    power = 2
    toughness = 1

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FIRST_STRIKE, Filters.Self),
            condition = Conditions.IsYourTurn,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Sidharth Chaturvedi"
        flavorText = "\"I don't understand. He's normally so well behaved!\""
        imageUri = "https://cards.scryfall.io/normal/front/9/3/9383956f-90bc-40e4-ae5c-503e98e21832.jpg?1783933478"
    }
}
