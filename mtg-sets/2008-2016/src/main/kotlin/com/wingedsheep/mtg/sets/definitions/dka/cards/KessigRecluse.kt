package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Kessig Recluse
 * {2}{G}{G}
 * Creature — Spider
 * 2/3
 * Reach
 * Deathtouch
 */
val KessigRecluse = card("Kessig Recluse") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spider"
    oracleText = "Reach (This creature can block creatures with flying.)\n" +
        "Deathtouch (Any amount of damage this deals to a creature is enough to destroy it.)"
    power = 2
    toughness = 3
    keywords(Keyword.REACH, Keyword.DEATHTOUCH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "121"
        artist = "Vincent Proce"
        flavorText = "Its silk is highly prized, but then again, so is your life."
        imageUri = "https://cards.scryfall.io/normal/front/6/9/695b8abe-796e-4d9b-aad3-4e03e925d2a7.jpg"
    }
}
