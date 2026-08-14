package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Barter in Blood reprint in Avacyn Restored. Canonical CardDefinition lives in Mirrodin (its
 * earliest real printing), `com.wingedsheep.mtg.sets.definitions.mrd.cards.BarterInBlood`.
 */
val BarterInBloodAvrReprint = Printing(
    oracleId = "9167998d-5cac-47d7-99f2-f38122f7b8e7",
    name = "Barter in Blood",
    setCode = "AVR",
    collectorNumber = "85",
    scryfallId = "39b4fab6-73ce-4a56-a305-4d2e93dbb4ee",
    artist = "Eric Deschamps",
    imageUri = "https://cards.scryfall.io/normal/front/3/9/39b4fab6-73ce-4a56-a305-4d2e93dbb4ee.jpg?1783940706",
    releaseDate = "2012-05-04",
    rarity = Rarity.UNCOMMON,
)
