package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Steam Vents
 * Land — Island Mountain
 *
 * ({T}: Add {U} or {R}.)
 * As this land enters, you may pay 2 life. If you don't, it enters tapped.
 */
val SteamVents = card("Steam Vents") {
    typeLine = "Land — Island Mountain"
    oracleText = "({T}: Add {U} or {R}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."

    // Mana abilities are intrinsic from basic land types (Island → {U}, Mountain → {R})

    replacementEffect(EntersTapped(payLifeCost = 2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "267"
        artist = "Adam Paquette"
        flavorText = "The flamekin was drawn toward warm laughter when the skies darkened, then she turned back to the lonely peak."
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b66daa94-d367-4812-9f18-f35378c1febb.jpg?1759144847"
    }
}
