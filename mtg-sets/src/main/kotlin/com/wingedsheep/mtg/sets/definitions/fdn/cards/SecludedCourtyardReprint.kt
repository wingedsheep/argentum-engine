package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Secluded Courtyard reprint in FDN.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in Kamigawa: Neon Dynasty's
 * `cards/` package (its earliest real printing). This file contributes only the FDN-specific
 * presentation row — set, collector number, art — picked up automatically by
 * `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SecludedCourtyardReprint = Printing(
    oracleId = "79ba18fd-f184-43c1-86df-56ee18ce806c",
    name = "Secluded Courtyard",
    setCode = "FDN",
    collectorNumber = "267",
    scryfallId = "d13373d2-139b-48c7-a8c9-828cefc4f150",
    artist = "Sam Burley",
    imageUri = "https://cards.scryfall.io/normal/front/d/1/d13373d2-139b-48c7-a8c9-828cefc4f150.jpg?1783909044",
    releaseDate = "2024-11-15",
    rarity = Rarity.UNCOMMON,
)
