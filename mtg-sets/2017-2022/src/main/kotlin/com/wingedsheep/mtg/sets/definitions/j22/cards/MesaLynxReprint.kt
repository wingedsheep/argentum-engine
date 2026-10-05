package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mesa Lynx reprint in J22. Canonical CardDefinition lives in Zendikar Rising (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.znr.cards.MesaLynx`.
 */
val MesaLynxReprint = Printing(
    oracleId = "211add65-b63d-43ad-8d69-b0598e003fef",
    name = "Mesa Lynx",
    setCode = "J22",
    collectorNumber = "214",
    scryfallId = "9c9cfcac-4912-4b34-b00e-16a18ee49823",
    artist = "Svetlin Velinov",
    imageUri = "https://cards.scryfall.io/normal/front/9/c/9c9cfcac-4912-4b34-b00e-16a18ee49823.jpg?1783919101",
    releaseDate = "2022-12-02",
    rarity = Rarity.COMMON,
)
