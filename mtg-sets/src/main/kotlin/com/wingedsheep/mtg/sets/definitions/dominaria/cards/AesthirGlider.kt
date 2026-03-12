package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock

/**
 * Aesthir Glider
 * {3}
 * Artifact Creature — Bird Construct
 * 2/1
 * Flying
 * This creature can't block.
 */
val AesthirGlider = card("Aesthir Glider") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Bird Construct"
    power = 2
    toughness = 1
    oracleText = "Flying\nThis creature can't block."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CantBlock()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "209"
        artist = "James Paick"
        flavorText = "An ancient device recovered from a thawing glacier high in the Karplusan mountains."
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25afd4a7-4253-4a4e-a105-e92f64460faa.jpg?1562732855"
    }
}
