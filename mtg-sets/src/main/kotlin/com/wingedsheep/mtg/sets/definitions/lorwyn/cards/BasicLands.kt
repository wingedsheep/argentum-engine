package com.wingedsheep.mtg.sets.definitions.lorwyn.cards

import com.wingedsheep.sdk.dsl.basicLand

/**
 * Lorwyn Eclipsed Basic Lands
 *
 * Contains 3 art variants of each basic land type.
 * Cards 269-283 (Plains 269/274/279, Island 270/275/280, Swamp 271/276/281,
 * Mountain 272/277/282, Forest 273/278/283)
 */

// =============================================================================
// Plains (Cards 269, 274, 279)
// =============================================================================

val Plains269 = basicLand("Plains") {
    collectorNumber = "269"
    artist = "Zoltan Boros"
    imageUri = "https://cards.scryfall.io/normal/front/3/a/3a438199-54f8-4702-81cf-a9d42e7cd9f1.jpg"
}

val Plains274 = basicLand("Plains") {
    collectorNumber = "274"
    artist = "Justin Gerard"
    imageUri = "https://cards.scryfall.io/normal/front/6/2/6242dbef-8412-4ebd-9486-42cec1dc6794.jpg"
}

val Plains279 = basicLand("Plains") {
    collectorNumber = "279"
    artist = "Justin Gerard"
    imageUri = "https://cards.scryfall.io/normal/front/d/8/d8fdfa7d-fd11-4c11-8743-a21538474314.jpg"
}

// =============================================================================
// Island (Cards 270, 275, 280)
// =============================================================================

val Island270 = basicLand("Island") {
    collectorNumber = "270"
    artist = "Ron Spears"
    imageUri = "https://cards.scryfall.io/normal/front/b/0/b0da67fb-1cb2-4105-ab5c-b7c680b8116c.jpg"
}

val Island275 = basicLand("Island") {
    collectorNumber = "275"
    artist = "Annie Stegg"
    imageUri = "https://cards.scryfall.io/normal/front/1/2/12ebd9c8-6587-456e-aba9-7aad1c2a09ea.jpg"
}

val Island280 = basicLand("Island") {
    collectorNumber = "280"
    artist = "Annie Stegg"
    imageUri = "https://cards.scryfall.io/normal/front/d/6/d6a5ba11-3156-4a0c-958d-5756e18b767b.jpg"
}

// =============================================================================
// Swamp (Cards 271, 276, 281)
// =============================================================================

val Swamp271 = basicLand("Swamp") {
    collectorNumber = "271"
    artist = "Jorge Jacinto"
    imageUri = "https://cards.scryfall.io/normal/front/1/d/1dd4d605-02a2-4183-b191-0bca8dfbf962.jpg"
}

val Swamp276 = basicLand("Swamp") {
    collectorNumber = "276"
    artist = "Raoul Vitale"
    imageUri = "https://cards.scryfall.io/normal/front/0/c/0cf904da-496c-4d88-a62b-c736ba895078.jpg"
}

val Swamp281 = basicLand("Swamp") {
    collectorNumber = "281"
    artist = "Raoul Vitale"
    imageUri = "https://cards.scryfall.io/normal/front/3/5/35fe42f9-dd55-4ca4-af0a-59ecdff0dba8.jpg"
}

// =============================================================================
// Mountain (Cards 272, 277, 282)
// =============================================================================

val Mountain272 = basicLand("Mountain") {
    collectorNumber = "272"
    artist = "Raymond Bonilla"
    imageUri = "https://cards.scryfall.io/normal/front/2/9/295b92bc-d66f-45d8-9bbe-5f5f13e39fd4.jpg"
}

val Mountain277 = basicLand("Mountain") {
    collectorNumber = "277"
    artist = "Ralph Horsley"
    imageUri = "https://cards.scryfall.io/normal/front/9/b/9b6fe3e2-a2cf-4209-ac9b-e04d02599360.jpg"
}

val Mountain282 = basicLand("Mountain") {
    collectorNumber = "282"
    artist = "Ralph Horsley"
    imageUri = "https://cards.scryfall.io/normal/front/9/6/96e14936-5614-46ba-874c-c1357243fe02.jpg"
}

// =============================================================================
// Forest (Cards 273, 278, 283)
// =============================================================================

val Forest273 = basicLand("Forest") {
    collectorNumber = "273"
    artist = "Jorge Jacinto"
    imageUri = "https://cards.scryfall.io/normal/front/b/4/b460f5f7-c7c9-400c-8419-23d614f45bf9.jpg"
}

val Forest278 = basicLand("Forest") {
    collectorNumber = "278"
    artist = "Jason Mowry"
    imageUri = "https://cards.scryfall.io/normal/front/b/4/b43297de-7378-474f-a85f-910fa7cbb4f4.jpg"
}

val Forest283 = basicLand("Forest") {
    collectorNumber = "283"
    artist = "Jason Mowry"
    imageUri = "https://cards.scryfall.io/normal/front/b/d/bdbce923-c05e-4554-8c4c-5c4e6d791856.jpg"
}

/**
 * All Lorwyn Eclipsed basic land variants.
 */
val LorwynEclipsedBasicLands = listOf(
    Plains269, Plains274, Plains279,
    Island270, Island275, Island280,
    Swamp271, Swamp276, Swamp281,
    Mountain272, Mountain277, Mountain282,
    Forest273, Forest278, Forest283
)
