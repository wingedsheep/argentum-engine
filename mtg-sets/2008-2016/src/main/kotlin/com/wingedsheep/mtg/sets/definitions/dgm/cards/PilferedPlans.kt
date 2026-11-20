package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Pilfered Plans
 * {1}{U}{B}
 * Sorcery
 * Target player mills two cards. Draw two cards.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val PilferedPlans = card("Pilfered Plans") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Target player mills two cards. Draw two cards."

    spell {
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(2, player) then Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "90"
        artist = "Michael C. Hayes"
        flavorText = "Mirko Vosk hunted anyone with knowledge of the maze, draining the chances of the other guilds."
        imageUri = "https://cards.scryfall.io/normal/front/3/4/3475fcc6-ee53-48da-89d2-80685a584e6a.jpg"
    }
}
