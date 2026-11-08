package com.wingedsheep.mtg.sets.definitions.acr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fatal Push reprints in Assassin's Creed (ACR). Canonical CardDefinition lives in Aether Revolt (AER);
 * this file contributes only presentation data.
 */
val FatalPushReprint = Printing(
    oracleId = "16437a83-be52-44cd-a768-a767c9347eb2",
    name = "Fatal Push",
    setCode = "ACR",
    collectorNumber = "90",
    scryfallId = "e6949e85-2db2-43a7-93bd-485c7b196298",
    artist = "Lie Setiawan",
    imageUri = "https://cards.scryfall.io/normal/front/e/6/e6949e85-2db2-43a7-93bd-485c7b196298.jpg?1783910959",
    releaseDate = "2024-07-05",
    rarity = Rarity.UNCOMMON,
)

val FatalPushReprint2 = Printing(
    oracleId = "16437a83-be52-44cd-a768-a767c9347eb2",
    name = "Fatal Push",
    setCode = "ACR",
    collectorNumber = "204",
    scryfallId = "841fefda-5da3-455e-9f11-1ef5c97c1b0d",
    artist = "Lie Setiawan",
    imageUri = "https://cards.scryfall.io/normal/front/8/4/841fefda-5da3-455e-9f11-1ef5c97c1b0d.jpg?1783910922",
    releaseDate = "2024-07-05",
    rarity = Rarity.UNCOMMON,
    frameEffects = listOf("inverted", "etched"),
)
