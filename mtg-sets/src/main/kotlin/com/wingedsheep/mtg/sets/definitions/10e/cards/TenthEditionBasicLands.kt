package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.dsl.basicLand

/**
 * Tenth Edition Basic Lands
 *
 * Tenth Edition printed four art variants of each basic land type (cards 364-383). Our Scryfall
 * cache retains one canonical variant per type, so we register that representative printing for
 * each: Plains 364, Island 368, Swamp 372, Mountain 376, Forest 380.
 */

val Ed10Plains364 = basicLand("Plains") {
    collectorNumber = "364"
    artist = "Rob Alexander"
    imageUri = "https://cards.scryfall.io/normal/front/7/c/7cfd53b2-3991-4a57-81b5-46618aecce4a.jpg"
}

val Ed10Island368 = basicLand("Island") {
    collectorNumber = "368"
    artist = "Donato Giancola"
    imageUri = "https://cards.scryfall.io/normal/front/b/7/b74648fc-587c-4d20-9432-58465bd7dca9.jpg"
}

val Ed10Swamp372 = basicLand("Swamp") {
    collectorNumber = "372"
    artist = "John Avon"
    imageUri = "https://cards.scryfall.io/normal/front/f/6/f6497c73-4bbb-4923-8151-8234c98ca6d4.jpg"
}

val Ed10Mountain376 = basicLand("Mountain") {
    collectorNumber = "376"
    artist = "John Avon"
    imageUri = "https://cards.scryfall.io/normal/front/a/7/a7aae748-27d8-48f8-beb2-8e0192d7cc5c.jpg"
}

val Ed10Forest380 = basicLand("Forest") {
    collectorNumber = "380"
    artist = "Anthony S. Waters"
    imageUri = "https://cards.scryfall.io/normal/front/d/9/d99d5685-538d-4e6a-809b-3ebeab634363.jpg"
}

val TenthEditionBasicLands = listOf(
    Ed10Plains364,
    Ed10Island368,
    Ed10Swamp372,
    Ed10Mountain376,
    Ed10Forest380,
)
