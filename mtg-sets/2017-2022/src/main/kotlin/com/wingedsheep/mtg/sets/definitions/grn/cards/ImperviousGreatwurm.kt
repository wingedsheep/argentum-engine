package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Impervious Greatwurm
 * {7}{G}{G}{G}
 * Creature — Wurm
 * 16/16
 * Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)
 * Indestructible
 */
val ImperviousGreatwurm = card("Impervious Greatwurm") {
    manaCost = "{7}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wurm"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Indestructible"
    power = 16
    toughness = 16

    keywords(Keyword.CONVOKE, Keyword.INDESTRUCTIBLE)

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "273"
        inBooster = false
        artist = "Simon Dominic"
        flavorText = "The ultimate answer to intrigue and subtlety."
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5e6dc5f-fd83-4166-a5da-cd953722642a.jpg?1783934092"
    }
}
