package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sigiled Skink
 * {1}{R}
 * Creature — Lizard
 * 2/1
 *
 * Whenever this creature attacks, scry 1. (Look at the top card of your library. You may put that card on the bottom.)
 */
val SigiledSkink = card("Sigiled Skink") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard"
    power = 2
    toughness = 1
    oracleText = "Whenever this creature attacks, scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Yeong-Hao Han"
        flavorText = "The runes seem to come alive as it moves, rippling like slow flames across its scales."
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37f84b4d-82db-4e3a-a099-2e438c0a6418.jpg?1783939419"
    }
}
