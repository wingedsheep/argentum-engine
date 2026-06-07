package com.wingedsheep.mtg.sets.definitions.cmd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Gruul Signet reprint in Commander 2011. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Guildpact set's `cards/`
 * package (Guildpact is the card's earliest real printing); this file contributes
 * only presentation data for the Commander 2011 printing.
 */
val GruulSignetReprint = Printing(
    oracleId = "d36e0c9f-c025-4dfe-9644-9cad2461ce38",
    name = "Gruul Signet",
    setCode = "CMD",
    collectorNumber = "250",
    scryfallId = "a677212a-c275-41e3-90c9-be2a866f8091",
    artist = "Greg Hildebrandt",
    imageUri = "https://cards.scryfall.io/normal/front/a/6/a677212a-c275-41e3-90c9-be2a866f8091.jpg?1592714439",
    releaseDate = "2011-06-17",
    rarity = Rarity.COMMON,
)
