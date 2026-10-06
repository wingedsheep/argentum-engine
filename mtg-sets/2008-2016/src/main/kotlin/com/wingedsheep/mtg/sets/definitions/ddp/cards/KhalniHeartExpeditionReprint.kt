package com.wingedsheep.mtg.sets.definitions.ddp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Khalni Heart Expedition reprint in DDP. Canonical CardDefinition lives in Zendikar (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.zen.cards.KhalniHeartExpedition`.
 */
val KhalniHeartExpeditionReprint = Printing(
    oracleId = "be6155de-c5b2-415c-ad83-142f9926462a",
    name = "Khalni Heart Expedition",
    setCode = "DDP",
    collectorNumber = "18",
    scryfallId = "db7bca99-4e53-48bf-8b58-39f27cd41314",
    artist = "Jason Chan",
    imageUri = "https://cards.scryfall.io/normal/front/d/b/db7bca99-4e53-48bf-8b58-39f27cd41314.jpg?1783938257",
    releaseDate = "2015-08-28",
    rarity = Rarity.COMMON,
)
