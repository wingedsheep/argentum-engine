package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Scourge Wolf (Shadows over Innistrad #179)
 * {R}{R}
 * Creature — Wolf Horror
 * 2/2
 *
 * First strike
 * Delirium — This creature has double strike as long as there are four or more card types among
 * cards in your graveyard.
 */
val ScourgeWolf = card("Scourge Wolf") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Wolf Horror"
    power = 2
    toughness = 2
    oracleText = "First strike\n" +
        "Delirium — This creature has double strike as long as there are four or more card types " +
        "among cards in your graveyard."

    keywords(Keyword.FIRST_STRIKE)

    staticAbility {
        ability = GrantKeyword(Keyword.DOUBLE_STRIKE, Filters.Self)
        condition = Conditions.Delirium()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "179"
        artist = "Jama Jurabaev"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/072f47ad-3055-4bd2-b7fd-cfbd22720d58.jpg"
    }
}
