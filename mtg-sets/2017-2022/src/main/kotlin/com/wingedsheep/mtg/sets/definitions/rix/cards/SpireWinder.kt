package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Spire Winder — Rivals of Ixalan #57
 * {3}{U} · Creature — Snake · 2/3
 *
 * Flying
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)
 * This creature gets +1/+1 as long as you have the city's blessing.
 *
 * Snubhorn Sentry's shape: the Ascend keyword plus a blessing-gated [ConditionalStaticAbility] on the
 * source's own stats.
 */
val SpireWinder = card("Spire Winder") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Snake"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "This creature gets +1/+1 as long as you have the city's blessing."

    keywords(Keyword.FLYING, Keyword.ASCEND)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 1, toughnessBonus = 1, filter = GroupFilter.source()),
            condition = Conditions.YouHaveCitysBlessing
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Titus Lunter"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/aca438d7-1134-4fee-9fad-85c3b1abe8ed.jpg?1783935317"
        ruling(
            "2018-01-19",
            "Once you have the city's blessing, you have it for the rest of the game, even if you lose control of some or all of your permanents. " +
                "The city's blessing isn't a permanent itself and can't be removed by any effect."
        )
    }
}
