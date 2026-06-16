package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.basicLand

/**
 * Secrets of Strixhaven Basic Lands
 *
 * Secrets of Strixhaven contains 3 art variants of each basic land type.
 * Cards 267-281.
 */

// =============================================================================
// Plains (Cards 267, 272, 273)
// =============================================================================

val SecretsOfStrixhavenPlains267 = basicLand("Plains") {
    collectorNumber = "267"
    artist = "Joshua Raphael"
    imageUri = "https://cards.scryfall.io/normal/front/a/8/a845de50-4af0-4f4a-9c2a-db587973571c.jpg?1775938869"
}

val SecretsOfStrixhavenPlains272 = basicLand("Plains") {
    collectorNumber = "272"
    artist = "Leon Tukker"
    imageUri = "https://cards.scryfall.io/normal/front/d/8/d85d0f25-a24a-4de0-9b8b-93fb5017bce9.jpg?1775938910"
}

val SecretsOfStrixhavenPlains273 = basicLand("Plains") {
    collectorNumber = "273"
    artist = "Andreas Zafiratos"
    imageUri = "https://cards.scryfall.io/normal/front/b/a/bac08825-ca54-4a9f-bf4f-f75708a1550b.jpg?1775938916"
}

// =============================================================================
// Island (Cards 268, 274, 275)
// =============================================================================

val SecretsOfStrixhavenIsland268 = basicLand("Island") {
    collectorNumber = "268"
    artist = "Joshua Raphael"
    imageUri = "https://cards.scryfall.io/normal/front/9/3/937250fe-bcad-4ff8-9406-286a69db7e0a.jpg?1775938878"
}

val SecretsOfStrixhavenIsland274 = basicLand("Island") {
    collectorNumber = "274"
    artist = "Sergey Glushakov"
    imageUri = "https://cards.scryfall.io/normal/front/7/7/77b88bb8-6bd9-4632-b937-89468fcb5e6a.jpg?1775938923"
}

val SecretsOfStrixhavenIsland275 = basicLand("Island") {
    collectorNumber = "275"
    artist = "Constantin Marin"
    imageUri = "https://cards.scryfall.io/normal/front/f/d/fd1b9e7c-09d0-4907-9f48-34289f3cd2cc.jpg?1775938929"
}

// =============================================================================
// Swamp (Cards 269, 276, 277)
// =============================================================================

val SecretsOfStrixhavenSwamp269 = basicLand("Swamp") {
    collectorNumber = "269"
    artist = "Joshua Raphael"
    imageUri = "https://cards.scryfall.io/normal/front/1/7/1797d5c7-d3fa-4184-85ae-46db14ddf523.jpg?1775938885"
}

val SecretsOfStrixhavenSwamp276 = basicLand("Swamp") {
    collectorNumber = "276"
    artist = "Sergey Glushakov"
    imageUri = "https://cards.scryfall.io/normal/front/5/1/51fe930f-2b5a-4b1e-9007-6ee74fb44715.jpg?1775938936"
}

val SecretsOfStrixhavenSwamp277 = basicLand("Swamp") {
    collectorNumber = "277"
    artist = "Leon Tukker"
    imageUri = "https://cards.scryfall.io/normal/front/3/6/36af939c-11d9-43a5-bd69-c915a62e972b.jpg?1775938944"
}

// =============================================================================
// Mountain (Cards 270, 278, 279)
// =============================================================================

val SecretsOfStrixhavenMountain270 = basicLand("Mountain") {
    collectorNumber = "270"
    artist = "Joshua Raphael"
    imageUri = "https://cards.scryfall.io/normal/front/6/a/6af1f1db-eb91-4297-83f6-9318b87fd220.jpg?1775938894"
}

val SecretsOfStrixhavenMountain278 = basicLand("Mountain") {
    collectorNumber = "278"
    artist = "Sergey Glushakov"
    imageUri = "https://cards.scryfall.io/normal/front/a/6/a642c7b1-d4d1-4125-a66d-560438e5ee51.jpg?1775938951"
}

val SecretsOfStrixhavenMountain279 = basicLand("Mountain") {
    collectorNumber = "279"
    artist = "Florian Herold"
    imageUri = "https://cards.scryfall.io/normal/front/b/c/bce45bc0-accd-4853-a166-9dd5597526d5.jpg?1775938958"
}

// =============================================================================
// Forest (Cards 271, 280, 281)
// =============================================================================

val SecretsOfStrixhavenForest271 = basicLand("Forest") {
    collectorNumber = "271"
    artist = "Joshua Raphael"
    imageUri = "https://cards.scryfall.io/normal/front/4/6/46196e8f-9339-4f00-b9cf-cab8f9abc80e.jpg?1775938902"
}

val SecretsOfStrixhavenForest280 = basicLand("Forest") {
    collectorNumber = "280"
    artist = "Raph Lomotan"
    imageUri = "https://cards.scryfall.io/normal/front/f/1/f169dfb2-e4c8-46e9-8591-e51bb82da082.jpg?1775938964"
}

val SecretsOfStrixhavenForest281 = basicLand("Forest") {
    collectorNumber = "281"
    artist = "Andreas Zafiratos"
    imageUri = "https://cards.scryfall.io/normal/front/3/0/3041d539-4f15-4836-a215-afa19a5cc23f.jpg?1775938970"
}

/**
 * All Secrets of Strixhaven basic land variants.
 */
val SecretsOfStrixhavenBasicLands = listOf(
    SecretsOfStrixhavenPlains267, SecretsOfStrixhavenPlains272, SecretsOfStrixhavenPlains273,
    SecretsOfStrixhavenIsland268, SecretsOfStrixhavenIsland274, SecretsOfStrixhavenIsland275,
    SecretsOfStrixhavenSwamp269, SecretsOfStrixhavenSwamp276, SecretsOfStrixhavenSwamp277,
    SecretsOfStrixhavenMountain270, SecretsOfStrixhavenMountain278, SecretsOfStrixhavenMountain279,
    SecretsOfStrixhavenForest271, SecretsOfStrixhavenForest280, SecretsOfStrixhavenForest281,
)
