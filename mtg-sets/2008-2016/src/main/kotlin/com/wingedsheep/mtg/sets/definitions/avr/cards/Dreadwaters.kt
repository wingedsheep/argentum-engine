package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Dreadwaters
 * {3}{U}
 * Sorcery
 * Target player mills X cards, where X is the number of lands you control.
 */
val Dreadwaters = card("Dreadwaters") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Target player mills X cards, where X is the number of lands you control."

    spell {
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(DynamicAmounts.landsYouControl(), player)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "Cliff Childs"
        flavorText = "Stray into the Morkrut and it may rise to claim you."
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88245a41-d4d5-46bf-969f-48d4dd540e2c.jpg?1783940724"
        ruling("2012-05-01", "The number of cards put into the graveyard is based on the number of lands you control when Dreadwaters resolves.")
    }
}
