package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Gilded Lotus reprint in Dominaria. Canonical CardDefinition lives in its earliest set, Mirrodin.
 *
 * Dominaria previously carried the canonical `card(...)` because Mirrodin had not been scaffolded
 * for this card yet; M13, BLC and FDN were already `Printing(...)` rows pointing at "its earliest
 * set". Adding the Mirrodin canonical moved Dominaria into line with them.
 */
val GildedLotusReprint = Printing(
    oracleId = "9a02a9a7-39d9-4763-85d3-747a0540b60b",
    name = "Gilded Lotus",
    setCode = "DOM",
    collectorNumber = "215",
    scryfallId = "a487e208-8493-4bca-8c44-284d89c66b15",
    artist = "Volkan Baǵa",
    imageUri = "https://cards.scryfall.io/normal/front/a/4/a487e208-8493-4bca-8c44-284d89c66b15.jpg?1783934959",
    releaseDate = "2018-04-27",
    rarity = Rarity.RARE,
)
