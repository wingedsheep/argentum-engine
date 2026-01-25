package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EachPlayerDiscardsDrawsEffect

/**
 * Flux
 * {2}{U}
 * Sorcery
 * Each player discards any number of cards, then draws that many cards. Draw a card.
 */
val Flux = card("Flux") {
    manaCost = "{2}{U}"
    typeLine = "Sorcery"

    spell {
        effect = EachPlayerDiscardsDrawsEffect(controllerBonusDraw = 1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Ted Naifeh"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c26bf66-8fa8-4f69-9556-c9fcc56a7f33.jpg"
    }
}
