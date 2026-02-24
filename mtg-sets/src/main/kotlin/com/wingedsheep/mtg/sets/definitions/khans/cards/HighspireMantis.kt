package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Highspire Mantis
 * {2}{R}{W}
 * Creature — Insect
 * 3/3
 * Flying, trample
 */
val HighspireMantis = card("Highspire Mantis") {
    manaCost = "{2}{R}{W}"
    typeLine = "Creature — Insect"
    power = 3
    toughness = 3
    oracleText = "Flying, trample"

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Igor Kieryluk"
        flavorText = "Its wings produce a high-pitched, barely audible whirring sound in flight. Only Jeskai masters are quiet enough to hear one coming."
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60d3708b-dd40-4515-bf8f-36cbc5de6b67.jpg?1562787429"
    }
}
