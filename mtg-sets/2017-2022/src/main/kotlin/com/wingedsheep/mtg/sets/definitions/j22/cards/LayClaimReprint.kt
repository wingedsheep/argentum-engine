package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lay Claim reprint in J22. Canonical CardDefinition lives in Amonkhet (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.akh.cards.LayClaim`.
 */
val LayClaimReprint = Printing(
    oracleId = "c7111365-b96b-4d6a-8c3c-73a0d8baf641",
    name = "Lay Claim",
    setCode = "J22",
    collectorNumber = "312",
    scryfallId = "e3d2ab42-8178-4a6d-9605-976a430491ef",
    artist = "Chris Rallis",
    imageUri = "https://cards.scryfall.io/normal/front/e/3/e3d2ab42-8178-4a6d-9605-976a430491ef.jpg?1783919054",
    releaseDate = "2022-12-02",
    rarity = Rarity.UNCOMMON,
)
