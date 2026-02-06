package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.basicLand

/**
 * Onslaught Basic Lands
 *
 * Onslaught contains 4 art variants of each basic land type.
 * Cards 331-350 (Plains 331-334, Island 335-338, Swamp 339-342, Mountain 343-346, Forest 347-350)
 */

// =============================================================================
// Plains (Cards 331-334)
// =============================================================================

val Plains331 = basicLand("Plains") {
    collectorNumber = "331"
    artist = "Rob Alexander"
    imageUri = "https://cards.scryfall.io/normal/front/7/b/7bf7d68a-dbd0-45f3-acbb-59ee38e6057e.jpg"
}

val Plains332 = basicLand("Plains") {
    collectorNumber = "332"
    artist = "Matthew Mitchell"
    imageUri = "https://cards.scryfall.io/normal/front/e/5/e52ed647-bd30-40a5-b648-0b98d1a3fd4a.jpg"
}

val Plains333 = basicLand("Plains") {
    collectorNumber = "333"
    artist = "David Martin"
    imageUri = "https://cards.scryfall.io/normal/front/8/5/854a255e-fd89-4c5d-b97b-416a9ac70960.jpg"
}

val Plains334 = basicLand("Plains") {
    collectorNumber = "334"
    artist = "David Day"
    imageUri = "https://cards.scryfall.io/normal/front/f/d/fd7babbe-f8c1-4e7c-8de2-2224dd357de4.jpg"
}

// =============================================================================
// Island (Cards 335-338)
// =============================================================================

val Island335 = basicLand("Island") {
    collectorNumber = "335"
    artist = "Tony Szczudlo"
    imageUri = "https://cards.scryfall.io/normal/front/3/6/36e062ec-df51-40c0-ad8a-2ee1cb8f8f17.jpg"
}

val Island336 = basicLand("Island") {
    collectorNumber = "336"
    artist = "Bradley Williams"
    imageUri = "https://cards.scryfall.io/normal/front/6/e/6e8c0e52-8482-4c33-bc5d-26eaad922e72.jpg"
}

val Island337 = basicLand("Island") {
    collectorNumber = "337"
    artist = "Matt Thompson"
    imageUri = "https://cards.scryfall.io/normal/front/1/d/1dac3bfe-884b-4875-bc7d-df564eb014cd.jpg"
}

val Island338 = basicLand("Island") {
    collectorNumber = "338"
    artist = "Randy Elliott"
    imageUri = "https://cards.scryfall.io/normal/front/1/8/189a09b8-46d2-4ef6-b7cc-9e510d1ea0b8.jpg"
}

// =============================================================================
// Swamp (Cards 339-342)
// =============================================================================

val Swamp339 = basicLand("Swamp") {
    collectorNumber = "339"
    artist = "Tony Szczudlo"
    imageUri = "https://cards.scryfall.io/normal/front/0/3/0356ae45-e5ca-46b9-8ebc-42bf4776e89c.jpg"
}

val Swamp340 = basicLand("Swamp") {
    collectorNumber = "340"
    artist = "Doug Chaffee"
    imageUri = "https://cards.scryfall.io/normal/front/a/6/a6285f63-a5d8-4b8b-a6dd-51ce7968fbaf.jpg"
}

val Swamp341 = basicLand("Swamp") {
    collectorNumber = "341"
    artist = "Dan Frazier"
    imageUri = "https://cards.scryfall.io/normal/front/7/a/7aa97b25-1ea0-4351-ab9f-f06c8bb4d044.jpg"
}

val Swamp342 = basicLand("Swamp") {
    collectorNumber = "342"
    artist = "Pete Venters"
    imageUri = "https://cards.scryfall.io/normal/front/8/e/8e10b125-eaa6-4630-a6fe-6b1805921f07.jpg"
}

// =============================================================================
// Mountain (Cards 343-346)
// =============================================================================

val Mountain343 = basicLand("Mountain") {
    collectorNumber = "343"
    artist = "Tony Szczudlo"
    imageUri = "https://cards.scryfall.io/normal/front/0/5/05f9bdca-0d54-46c7-b803-9083dfc9ee24.jpg"
}

val Mountain344 = basicLand("Mountain") {
    collectorNumber = "344"
    artist = "Sam Wood"
    imageUri = "https://cards.scryfall.io/normal/front/b/6/b6d39f35-c7b2-43b2-aee3-4ff2cd3e37e7.jpg"
}

val Mountain345 = basicLand("Mountain") {
    collectorNumber = "345"
    artist = "David Day"
    imageUri = "https://cards.scryfall.io/normal/front/e/8/e8aade2d-5cf5-44f6-9095-aa3756b1c1dd.jpg"
}

val Mountain346 = basicLand("Mountain") {
    collectorNumber = "346"
    artist = "Heather Hudson"
    imageUri = "https://cards.scryfall.io/normal/front/f/d/fd194fb1-0d3a-4eff-a446-240d18dad43c.jpg"
}

// =============================================================================
// Forest (Cards 347-350)
// =============================================================================

val Forest347 = basicLand("Forest") {
    collectorNumber = "347"
    artist = "John Avon"
    imageUri = "https://cards.scryfall.io/normal/front/b/3/b361b42d-401f-440a-bae9-35338b5dde0e.jpg"
}

val Forest348 = basicLand("Forest") {
    collectorNumber = "348"
    artist = "John Matson"
    imageUri = "https://cards.scryfall.io/normal/front/4/d/4d8edfee-7837-450a-bcf3-a7bb25670056.jpg"
}

val Forest349 = basicLand("Forest") {
    collectorNumber = "349"
    artist = "John Avon"
    imageUri = "https://cards.scryfall.io/normal/front/7/b/7b0af992-80e0-4ac6-a828-5eaac47eaff6.jpg"
}

val Forest350 = basicLand("Forest") {
    collectorNumber = "350"
    artist = "David Martin"
    imageUri = "https://cards.scryfall.io/normal/front/8/3/835a4eed-a308-428d-ac85-e385b5d47d8e.jpg"
}

/**
 * All Onslaught basic land variants.
 */
val OnslaughtBasicLands = listOf(
    Plains331, Plains332, Plains333, Plains334,
    Island335, Island336, Island337, Island338,
    Swamp339, Swamp340, Swamp341, Swamp342,
    Mountain343, Mountain344, Mountain345, Mountain346,
    Forest347, Forest348, Forest349, Forest350
)
