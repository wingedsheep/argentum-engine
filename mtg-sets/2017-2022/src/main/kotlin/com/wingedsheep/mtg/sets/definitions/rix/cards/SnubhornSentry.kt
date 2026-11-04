package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Snubhorn Sentry — Rivals of Ixalan #23
 * {W} · Creature — Dinosaur · 0/3
 *
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)
 * This creature gets +3/+0 as long as you have the city's blessing.
 *
 * Ascend is the keyword alone — the engine's state-based check grants the blessing. The bonus is a
 * [ConditionalStaticAbility] modifying the source only, gated on the blessing (Skymarcher Aspirant's
 * shape with a stat bonus in place of the keyword grant).
 */
val SnubhornSentry = card("Snubhorn Sentry") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dinosaur"
    power = 0
    toughness = 3
    oracleText = "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "This creature gets +3/+0 as long as you have the city's blessing."

    keywords(Keyword.ASCEND)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 3, toughnessBonus = 0, filter = GroupFilter.source()),
            condition = Conditions.YouHaveCitysBlessing
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "23"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3ee111e7-5309-46d5-b3ea-a049b3962c23.jpg?1783935332"
        ruling(
            "2018-01-19",
            "Once you have the city's blessing, you have it for the rest of the game, even if you lose control of some or all of your permanents. " +
                "The city's blessing isn't a permanent itself and can't be removed by any effect."
        )
    }
}
