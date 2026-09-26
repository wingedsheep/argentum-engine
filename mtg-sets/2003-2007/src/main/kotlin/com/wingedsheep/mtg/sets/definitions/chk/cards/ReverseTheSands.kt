package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Reverse the Sands — Champions of Kamigawa #41 (canonical printing)
 * {6}{W}{W} · Sorcery
 *
 * Redistribute any number of players' life totals. (Each of those players gets one life total back.)
 *
 * At resolution the caster hands each player one of the current life totals; a player handed their
 * own total is left out. Totals move whole — they can't be split.
 */
val ReverseTheSands = card("Reverse the Sands") {
    manaCost = "{6}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Redistribute any number of players' life totals. (Each of those players gets one life total back.)"

    spell {
        effect = Effects.RedistributeLifeTotals()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "41"
        artist = "Jeremy Jarvis"
        flavorText = "Worse than the years of aging was the burden of memory. The young monk lost his youth " +
            "and gained a vicarious lifetime of hardship and woe."
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aae9c988-5e8c-4d3a-af6d-8ac083698159.jpg?1783944332"
        ruling("2004-12-01", "You choose which player gets which life total when the spell resolves.")
        ruling(
            "2004-12-01",
            "You can't split up a life total when you redistribute it. For example, suppose that in a " +
                "two-player game your life total is 5 and your opponent's life total is 15 when Reverse the " +
                "Sands starts to resolve. You can choose to (a) leave the life totals as they are or (b) make " +
                "your life total 15 and your opponent's 5. You can't choose to make your life total 20 and " +
                "your opponent's 0."
        )
    }
}
