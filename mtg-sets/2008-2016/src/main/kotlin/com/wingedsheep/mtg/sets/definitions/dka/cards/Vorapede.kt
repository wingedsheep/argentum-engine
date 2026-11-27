package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Vorapede
 * {2}{G}{G}{G}
 * Creature — Insect
 * 5/4
 * Vigilance, trample
 * Undying
 */
val Vorapede = card("Vorapede") {
    manaCost = "{2}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect"
    oracleText = "Vigilance, trample\n" +
        "Undying (When this creature dies, if it had no +1/+1 counters on it, return it to the battlefield under its owner's control with a +1/+1 counter on it.)"
    power = 5
    toughness = 4
    keywords(Keyword.VIGILANCE, Keyword.TRAMPLE, Keyword.UNDYING)

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "131"
        artist = "Slawomir Maniak"
        flavorText = "\"If you think you've killed one, guess again.\"\n—Thalia, Knight-Cathar"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/1348aa65-85e7-4ac7-bcdb-a83f2c3aa1a6.jpg"
    }
}
