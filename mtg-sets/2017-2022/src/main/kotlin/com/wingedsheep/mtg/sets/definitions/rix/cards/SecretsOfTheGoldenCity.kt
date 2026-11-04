package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Secrets of the Golden City
 * {1}{U}{U}
 * Sorcery
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)
 * Draw two cards. If you have the city's blessing, draw three cards instead.
 *
 * Ascend on a sorcery is a spell ability, so it is spelled out as the first step of resolution
 * (the Golden Demise shape): ten or more permanents grant [Effects.GainCitysBlessing], and the draw
 * then reads `Conditions.YouHaveCitysBlessing`, which sees a blessing this spell just granted.
 */
val SecretsOfTheGoldenCity = card("Secrets of the Golden City") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "Draw two cards. If you have the city's blessing, draw three cards instead."

    keywords(Keyword.ASCEND)

    spell {
        effect = Effects.If(
            condition = Conditions.ControlPermanentsAtLeast(10),
            then = Effects.GainCitysBlessing()
        ) then Effects.If(
            condition = Conditions.YouHaveCitysBlessing,
            then = Effects.DrawCards(3),
            otherwise = Effects.DrawCards(2)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Jason Felix"
        flavorText = "Stolen knowledge can never be stolen back."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/899ecd7a-bfd6-42a4-8cb2-18bac40b4401.jpg"
        ruling(
            "2018-01-19",
            "If you cast a spell with ascend, you don't get the city's blessing until it resolves. Players may " +
                "respond to that spell by trying to change whether you get the city's blessing."
        )
        ruling(
            "2018-01-19",
            "Once you have the city's blessing, you have it for the rest of the game, even if you lose control " +
                "of some or all of your permanents. The city's blessing isn't a permanent itself and can't be " +
                "removed by any effect."
        )
    }
}
