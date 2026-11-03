package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Fugitive Listener
 * {2}{B}
 * Creature — El-Aurian Scout
 * 3/1
 * When this creature enters, each opponent discards a card.
 */
val FugitiveListener = card("Fugitive Listener") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — El-Aurian Scout"
    oracleText = "When this creature enters, each opponent discards a card."
    power = 3
    toughness = 1
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Hand.eachOpponentDiscards(1)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Bastien L. Deharme"
        flavorText = "A precious handful of El-Aurians survived the Borg invasion, scattered throughout the Galaxy with no planet to call home."
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd04c738-b4e3-481e-9a68-d257524bf40e.jpg?1793361283"
    }
}
