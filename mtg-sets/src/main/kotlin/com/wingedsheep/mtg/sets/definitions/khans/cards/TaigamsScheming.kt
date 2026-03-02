package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Taigam's Scheming
 * {1}{U}
 * Sorcery
 * Surveil 5.
 *
 * Oracle errata: Originally "Look at the top five cards of your library, then put
 * any number of them into your graveyard and the rest on top of your library in any order."
 * Updated to use the surveil keyword action.
 */
val TaigamsScheming = card("Taigam's Scheming") {
    manaCost = "{1}{U}"
    typeLine = "Sorcery"
    oracleText = "Surveil 5. (Look at the top five cards of your library, then put any number of them into your graveyard and the rest on top of your library in any order.)"

    spell {
        effect = Effects.Surveil(5)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Svetlin Velinov"
        flavorText = "\"The Jeskai would have me bow in restraint. So I have found a people unafraid of true power.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87f4b533-7113-4b36-9c7a-e4cf798c88a9.jpg?1665819837"
    }
}
