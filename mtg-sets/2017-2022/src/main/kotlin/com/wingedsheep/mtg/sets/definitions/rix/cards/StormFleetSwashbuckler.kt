package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Storm Fleet Swashbuckler — Rivals of Ixalan #117
 * {1}{R} · Creature — Human Pirate · 2/2
 *
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)
 * This creature has double strike as long as you have the city's blessing.
 *
 * The Skymarcher Aspirant shape: Ascend keyword plus a blessing-gated [ConditionalStaticAbility]
 * granting double strike to the source only.
 */
val StormFleetSwashbuckler = card("Storm Fleet Swashbuckler") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Pirate"
    power = 2
    toughness = 2
    oracleText = "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "This creature has double strike as long as you have the city's blessing."

    keywords(Keyword.ASCEND)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.DOUBLE_STRIKE, GroupFilter.source()),
            condition = Conditions.YouHaveCitysBlessing
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Kieran Yanner"
        flavorText = "\"A bolt to the eye, a blade to the neck. That's the way to clear the deck!\""
        imageUri = "https://cards.scryfall.io/normal/front/7/0/7093bd0e-3b98-4f46-9717-202ddd277bf5.jpg?1783935292"
        ruling(
            "2018-01-19",
            "Once you have the city's blessing, you have it for the rest of the game, even if you lose control of some or all of your permanents. " +
                "The city's blessing isn't a permanent itself and can't be removed by any effect."
        )
    }
}
