package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.PlayersPlayWithHandsRevealed

/**
 * Revelation
 * {G}
 * World Enchantment
 *
 * Players play with their hands revealed.
 *
 * The symmetric form of Seer's Vision's [com.wingedsheep.sdk.scripting.OpponentsPlayWithHandsRevealed]:
 * [PlayersPlayWithHandsRevealed] opens every player's hand to every other player, its controller's
 * own included.
 */
val Revelation = card("Revelation") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "World Enchantment"
    oracleText = "Players play with their hands revealed."

    staticAbility {
        ability = PlayersPlayWithHandsRevealed
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "202"
        artist = "Kaja Foglio"
        flavorText = "\"Many are in high place, and of renown: but mysteries are revealed unto the meek.\" —Ecclesiastes 3:19"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07892b6c-08d2-47b5-8d64-0e4d1bdc3080.jpg?1783948045"
    }
}
