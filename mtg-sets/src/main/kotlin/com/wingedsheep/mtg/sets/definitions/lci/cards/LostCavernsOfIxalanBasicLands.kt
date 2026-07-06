package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.basicLand

/**
 * The Lost Caverns of Ixalan Basic Lands
 *
 * One art variant of each basic land type, collector numbers 287-291.
 */

val LciPlains287 = basicLand("Plains") {
    collectorNumber = "287"
    artist = "Olga Tereshenko"
    imageUri = "https://cards.scryfall.io/normal/front/b/5/b5453630-bfff-4403-a9a9-49f1534e1d42.jpg?1782694382"
}

val LciIsland288 = basicLand("Island") {
    collectorNumber = "288"
    artist = "WFlemming Illustration"
    imageUri = "https://cards.scryfall.io/normal/front/3/3/338e5b63-1fee-4a7c-af9b-483d383f79b7.jpg?1782694382"
}

val LciSwamp289 = basicLand("Swamp") {
    collectorNumber = "289"
    artist = "Elektrodeko"
    imageUri = "https://cards.scryfall.io/normal/front/a/8/a825ac86-d642-42fd-b6aa-94aa804907d9.jpg?1782694382"
}

val LciMountain290 = basicLand("Mountain") {
    collectorNumber = "290"
    artist = "BEMOCS"
    imageUri = "https://cards.scryfall.io/normal/front/7/c/7cb82fdb-5090-45c0-ae67-4846667c8625.jpg?1782694381"
}

val LciForest291 = basicLand("Forest") {
    collectorNumber = "291"
    artist = "Matteo Bassini"
    imageUri = "https://cards.scryfall.io/normal/front/8/c/8c13cafb-3078-4856-a5b0-c38aace8a34a.jpg?1782694380"
}

/**
 * All The Lost Caverns of Ixalan basic land variants.
 */
val LostCavernsOfIxalanBasicLands = listOf(
    LciPlains287,
    LciIsland288,
    LciSwamp289,
    LciMountain290,
    LciForest291,
)
