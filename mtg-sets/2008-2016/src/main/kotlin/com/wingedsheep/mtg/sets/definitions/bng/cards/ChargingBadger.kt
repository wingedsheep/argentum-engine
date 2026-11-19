package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Charging Badger
 * {G}
 * Creature — Badger
 * 1/1
 *
 * Trample
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val ChargingBadger = card("Charging Badger") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Badger"
    power = 1
    toughness = 1
    oracleText = "Trample"

    keywords(Keyword.TRAMPLE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "118"
        artist = "Raoul Vitale"
        flavorText = "\"If the hierarchies of nature were determined by ferocity alone, the badger would be lord of the beasts.\"\n—Anthousa of Setessa"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d88ad54-43fc-45e0-8e00-db6be0c021ea.jpg?1783939538"
    }
}
