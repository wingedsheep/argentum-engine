package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.basicLand

/**
 * Lorwyn Eclipsed Basic Lands
 *
 * Cards 269-273 (one of each basic land type).
 */

val EclPlains = basicLand("Plains") {
    collectorNumber = "269"
    artist = "Zoltan Boros"
    imageUri = "https://cards.scryfall.io/normal/front/3/a/3a438199-54f8-4702-81cf-a9d42e7cd9f1.jpg?1767773831"
}

val EclIsland = basicLand("Island") {
    collectorNumber = "270"
    artist = "Ron Spears"
    imageUri = "https://cards.scryfall.io/normal/front/b/0/b0da67fb-1cb2-4105-ab5c-b7c680b8116c.jpg?1767773855"
}

val EclSwamp = basicLand("Swamp") {
    collectorNumber = "271"
    artist = "Jorge Jacinto"
    imageUri = "https://cards.scryfall.io/normal/front/1/d/1dd4d605-02a2-4183-b191-0bca8dfbf962.jpg?1767773902"
}

val EclMountain = basicLand("Mountain") {
    collectorNumber = "272"
    artist = "Raymond Bonilla"
    imageUri = "https://cards.scryfall.io/normal/front/2/9/295b92bc-d66f-45d8-9bbe-5f5f13e39fd4.jpg?1767773878"
}

val EclForest = basicLand("Forest") {
    collectorNumber = "273"
    artist = "Jorge Jacinto"
    imageUri = "https://cards.scryfall.io/normal/front/b/4/b460f5f7-c7c9-400c-8419-23d614f45bf9.jpg?1767773926"
}

/**
 * All Lorwyn Eclipsed basic land variants.
 */
val LorwynEclipsedBasicLands = listOf(
    EclPlains,
    EclIsland,
    EclSwamp,
    EclMountain,
    EclForest,
)
