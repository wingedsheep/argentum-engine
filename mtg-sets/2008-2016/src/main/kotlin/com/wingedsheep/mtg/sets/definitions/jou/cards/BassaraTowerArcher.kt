package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bassara Tower Archer
 * {G}{G}
 * Creature — Human Archer
 * 2/1
 *
 * Reach, hexproof
 */
val BassaraTowerArcher = card("Bassara Tower Archer") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Archer"
    power = 2
    toughness = 1
    oracleText = "Reach, hexproof"

    keywords(Keyword.REACH, Keyword.HEXPROOF)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Slawomir Maniak"
        flavorText = "Setessan warriors of Bassara Tower are known for their guerrilla tactics and skill with the bow. " +
            "Interlopers into the Nessian Wood do not get far."
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95a1e5c2-7f5b-4ae4-83d9-06e334ba57ea.jpg?1783939417"
    }
}
