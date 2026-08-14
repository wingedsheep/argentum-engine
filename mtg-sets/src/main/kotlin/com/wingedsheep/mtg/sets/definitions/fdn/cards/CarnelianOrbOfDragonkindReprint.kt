package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Carnelian Orb of Dragonkind reprint in FDN. Canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Commander Legends: Battle for Baldur's Gate
 * (CLB); this file contributes only the FDN presentation row.
 *
 * FDN prints it twice outside the Play Booster pool — #534 in the Beginner Box and #759 in the 2026
 * set extension. This row follows the printing Scryfall serves as the set default (#759).
 */
val CarnelianOrbOfDragonkindReprint = Printing(
    oracleId = "651c967c-8f71-4eb5-b22f-545e55ea050e",
    name = "Carnelian Orb of Dragonkind",
    setCode = "FDN",
    collectorNumber = "759",
    scryfallId = "6db2741d-2722-4eb7-b09d-0d81649c7ca2",
    artist = "Lars Grant-West",
    imageUri = "https://cards.scryfall.io/normal/front/6/d/6db2741d-2722-4eb7-b09d-0d81649c7ca2.jpg?1783903948",
    releaseDate = "2026-04-24",
    rarity = Rarity.COMMON,
)
