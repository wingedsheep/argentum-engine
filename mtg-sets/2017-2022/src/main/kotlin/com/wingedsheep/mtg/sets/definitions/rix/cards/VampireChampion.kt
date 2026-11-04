package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Vampire Champion
 * {3}{B}
 * Creature — Vampire Soldier
 * 3/3
 * Deathtouch (Any amount of damage this deals to a creature is enough to destroy it.)
 */
val VampireChampion = card("Vampire Champion") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Soldier"
    oracleText = "Deathtouch (Any amount of damage this deals to a creature is enough to destroy it.)"
    power = 3
    toughness = 3

    keywords(Keyword.DEATHTOUCH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "198"
        inBooster = false
        artist = "Craig J Spearing"
        flavorText = "\"These pirates must answer for their ancestors, who renounced the Church of Dusk and sailed from Torrezon as heretics.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d47f91ff-c916-4938-8e01-2c684004dd9a.jpg?1783935258"
    }
}
