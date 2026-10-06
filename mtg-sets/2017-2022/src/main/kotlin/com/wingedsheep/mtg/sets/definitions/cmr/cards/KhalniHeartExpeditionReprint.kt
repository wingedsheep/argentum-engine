package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Khalni Heart Expedition reprint in CMR. Canonical CardDefinition lives in Zendikar (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.zen.cards.KhalniHeartExpedition`.
 */
val KhalniHeartExpeditionReprint = Printing(
    oracleId = "be6155de-c5b2-415c-ad83-142f9926462a",
    name = "Khalni Heart Expedition",
    setCode = "CMR",
    collectorNumber = "428",
    scryfallId = "45cbbcfb-1b1b-4991-988d-17a22a08d5b0",
    artist = "Jason Chan",
    imageUri = "https://cards.scryfall.io/normal/front/4/5/45cbbcfb-1b1b-4991-988d-17a22a08d5b0.jpg?1783928706",
    releaseDate = "2020-11-20",
    rarity = Rarity.COMMON,
)
