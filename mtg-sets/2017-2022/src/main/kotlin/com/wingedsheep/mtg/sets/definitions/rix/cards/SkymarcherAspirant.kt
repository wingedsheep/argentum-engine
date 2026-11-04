package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Skymarcher Aspirant — Rivals of Ixalan #21
 * {W} · Creature — Vampire Soldier · 2/1
 *
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)
 * This creature has flying as long as you have the city's blessing.
 *
 * Ascend is the keyword alone — the engine's state-based check grants the blessing. The flying is a
 * [ConditionalStaticAbility] granting the keyword to the source only, gated on the blessing (the
 * Shaper Apprentice shape with Radiant Destiny's condition).
 */
val SkymarcherAspirant = card("Skymarcher Aspirant") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Vampire Soldier"
    power = 2
    toughness = 1
    oracleText = "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "This creature has flying as long as you have the city's blessing."

    keywords(Keyword.ASCEND)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FLYING, GroupFilter.source()),
            condition = Conditions.YouHaveCitysBlessing
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Winona Nelson"
        flavorText = "\"I was born to glory.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/4/9473190f-5e96-4cf7-a0fa-a77d916acc2e.jpg?1783935332"
        ruling(
            "2018-01-19",
            "Once you have the city's blessing, you have it for the rest of the game, even if you lose control of some or all of your permanents. " +
                "The city's blessing isn't a permanent itself and can't be removed by any effect."
        )
    }
}
