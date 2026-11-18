package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Cloaked Siren
 * {3}{U}
 * Creature — Siren
 * 3/2
 *
 * Flash
 * Flying
 */
val CloakedSiren = card("Cloaked Siren") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Siren"
    power = 3
    toughness = 2
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\nFlying"

    keywords(Keyword.FLASH, Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Mark Zug"
        flavorText = "\"That is not the voice of the wind singing on the stones.\"\n—Callaphe the mariner"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79a639aa-91f4-49df-8671-122710073084.jpg?1783939450"
    }
}
