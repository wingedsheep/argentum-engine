package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Feral Shadow
 * {2}{B}
 * Creature — Nightstalker
 * 2/1
 * Flying
 */
val FeralShadow = card("Feral Shadow") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Nightstalker"
    power = 2
    toughness = 1
    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Andrew Robinson"
        flavorText = "\"We are the shadow on the sun, the thorn on the rose, the pain that sharpens the blade.\" —Nightstalker's oath"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c46f4c00-6bf8-440b-9761-b17a0e36c27e.jpg"
    }
}
