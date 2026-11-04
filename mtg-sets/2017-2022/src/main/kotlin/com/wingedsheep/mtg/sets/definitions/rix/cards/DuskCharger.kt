package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Dusk Charger — Rivals of Ixalan #69
 * {3}{B} · Creature — Horse · 3/3
 *
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)
 * This creature gets +2/+2 as long as you have the city's blessing.
 *
 * Snubhorn Sentry's shape: the Ascend keyword plus a blessing-gated [ConditionalStaticAbility]
 * pumping the source.
 */
val DuskCharger = card("Dusk Charger") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horse"
    power = 3
    toughness = 3
    oracleText = "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "This creature gets +2/+2 as long as you have the city's blessing."

    keywords(Keyword.ASCEND)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 2, toughnessBonus = 2, filter = GroupFilter.source()),
            condition = Conditions.YouHaveCitysBlessing
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "69"
        artist = "Lucas Graciano"
        flavorText = "Stories say demon blood runs in its veins, making it fearless and bloodthirsty."
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2ce54a3-f3eb-48a0-bfa2-5f4b90e413f0.jpg?1783935312"
        ruling(
            "2018-01-19",
            "Once you have the city's blessing, you have it for the rest of the game, even if you lose control of some or all of your permanents. " +
                "The city's blessing isn't a permanent itself and can't be removed by any effect."
        )
    }
}
