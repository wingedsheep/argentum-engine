package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.basicLand

/**
 * Innistrad: Crimson Vow Basic Lands
 *
 * One full-art variant of each basic land type, collector numbers 408-412.
 */

val VowPlains408 = basicLand("Plains") {
    collectorNumber = "408"
    artist = "Daria Khlebnikova"
    imageUri = "https://cards.scryfall.io/normal/front/8/0/80897666-ade2-4f70-9c4d-235753b44a23.jpg?1782702899"
}

val VowIsland409 = basicLand("Island") {
    collectorNumber = "409"
    artist = "Rio Krisma"
    imageUri = "https://cards.scryfall.io/normal/front/1/f/1fddf4cb-0680-4bc7-8bb3-cb15268aff46.jpg?1782702899"
}

val VowSwamp410 = basicLand("Swamp") {
    collectorNumber = "410"
    artist = "Kerby Rosanes"
    imageUri = "https://cards.scryfall.io/normal/front/9/1/91afb4f0-70ef-4539-9081-dc130c7c63f5.jpg?1782702899"
}

val VowMountain411 = basicLand("Mountain") {
    collectorNumber = "411"
    artist = "Daria Khlebnikova"
    imageUri = "https://cards.scryfall.io/normal/front/1/1/117716bf-43c8-4534-92da-d7948d4b5628.jpg?1782702898"
}

val VowForest412 = basicLand("Forest") {
    collectorNumber = "412"
    artist = "Pig Hands"
    imageUri = "https://cards.scryfall.io/normal/front/a/7/a7279281-42d5-4226-b841-f1f4deff919b.jpg?1782702898"
}

/**
 * All Innistrad: Crimson Vow basic land variants.
 */
val InnistradCrimsonVowBasicLands = listOf(
    VowPlains408,
    VowIsland409,
    VowSwamp410,
    VowMountain411,
    VowForest412,
)
