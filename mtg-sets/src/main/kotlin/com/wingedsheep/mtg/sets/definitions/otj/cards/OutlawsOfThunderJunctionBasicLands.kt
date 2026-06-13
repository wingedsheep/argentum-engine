package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.basicLand

/**
 * Outlaws of Thunder Junction Basic Lands
 *
 * OTJ has a single full-art booster variant per type (272-276, Salvatorre Zee Yazzie) plus two
 * non-booster art variants per type (277-286, Sergey Glushakov / Adam Paquette). The non-booster
 * variants are kept defined for collection/display but excluded from the draft/sealed basic pool.
 * Cards 272-286.
 */

// =============================================================================
// Plains (Cards 272, 277, 278)
// =============================================================================

val OtjPlains272 = basicLand("Plains") {
    collectorNumber = "272"
    artist = "Salvatorre Zee Yazzie"
    imageUri = "https://cards.scryfall.io/normal/front/c/f/cfe51d97-66f6-4ac3-b926-01ab7e4c5686.jpg?1712356389"
}

val OtjPlains277 = basicLand("Plains") {
    collectorNumber = "277"
    artist = "Sergey Glushakov"
    imageUri = "https://cards.scryfall.io/normal/front/6/f/6f501773-1b39-4a4c-9b45-a950385c9e82.jpg?1712356413"
    inBooster = false
}

val OtjPlains278 = basicLand("Plains") {
    collectorNumber = "278"
    artist = "Adam Paquette"
    imageUri = "https://cards.scryfall.io/normal/front/f/e/fe60da77-084c-49e8-9948-9ac4b6a6382f.jpg?1712356416"
    inBooster = false
}

// =============================================================================
// Island (Cards 273, 279, 280)
// =============================================================================

val OtjIsland273 = basicLand("Island") {
    collectorNumber = "273"
    artist = "Salvatorre Zee Yazzie"
    imageUri = "https://cards.scryfall.io/normal/front/a/6/a624d656-207d-4e73-b615-59e7cf64ad64.jpg?1712356396"
}

val OtjIsland279 = basicLand("Island") {
    collectorNumber = "279"
    artist = "Sergey Glushakov"
    imageUri = "https://cards.scryfall.io/normal/front/a/c/acd6be3f-745c-41ad-95c9-1db66ba56be2.jpg?1712356421"
    inBooster = false
}

val OtjIsland280 = basicLand("Island") {
    collectorNumber = "280"
    artist = "Adam Paquette"
    imageUri = "https://cards.scryfall.io/normal/front/9/1/91be4db0-7cb3-4202-939b-cb7a26e90019.jpg?1712356423"
    inBooster = false
}

// =============================================================================
// Swamp (Cards 274, 281, 282)
// =============================================================================

val OtjSwamp274 = basicLand("Swamp") {
    collectorNumber = "274"
    artist = "Salvatorre Zee Yazzie"
    imageUri = "https://cards.scryfall.io/normal/front/3/b/3b383b16-8128-4d9e-a0d0-9b8ccc9ad6df.jpg?1712356398"
}

val OtjSwamp281 = basicLand("Swamp") {
    collectorNumber = "281"
    artist = "Sergey Glushakov"
    imageUri = "https://cards.scryfall.io/normal/front/e/b/eb7dc259-9949-4673-a8f1-874396948392.jpg?1712356428"
    inBooster = false
}

val OtjSwamp282 = basicLand("Swamp") {
    collectorNumber = "282"
    artist = "Adam Paquette"
    imageUri = "https://cards.scryfall.io/normal/front/5/c/5c2c9dc0-7f3a-4f3a-ba08-5c2f87e252bc.jpg?1712356431"
    inBooster = false
}

// =============================================================================
// Mountain (Cards 275, 283, 284)
// =============================================================================

val OtjMountain275 = basicLand("Mountain") {
    collectorNumber = "275"
    artist = "Salvatorre Zee Yazzie"
    imageUri = "https://cards.scryfall.io/normal/front/0/a/0a7dbfd2-cda7-4fc9-9677-e442fb5f5f6f.jpg?1712356404"
}

val OtjMountain283 = basicLand("Mountain") {
    collectorNumber = "283"
    artist = "Sergey Glushakov"
    imageUri = "https://cards.scryfall.io/normal/front/9/1/9137b4aa-2289-4a4d-b1f5-ae75a0928278.jpg?1712356436"
    inBooster = false
}

val OtjMountain284 = basicLand("Mountain") {
    collectorNumber = "284"
    artist = "Adam Paquette"
    imageUri = "https://cards.scryfall.io/normal/front/2/2/2237ee9b-fff6-472c-903c-11faf9bb116d.jpg?1712356440"
    inBooster = false
}

// =============================================================================
// Forest (Cards 276, 285, 286)
// =============================================================================

val OtjForest276 = basicLand("Forest") {
    collectorNumber = "276"
    artist = "Salvatorre Zee Yazzie"
    imageUri = "https://cards.scryfall.io/normal/front/b/a/baf8a774-65f3-431e-b084-328ff1000895.jpg?1712356407"
}

val OtjForest285 = basicLand("Forest") {
    collectorNumber = "285"
    artist = "Sergey Glushakov"
    imageUri = "https://cards.scryfall.io/normal/front/d/c/dc20a07e-5f92-49c2-9c97-d5eda536d3f6.jpg?1712356446"
    inBooster = false
}

val OtjForest286 = basicLand("Forest") {
    collectorNumber = "286"
    artist = "Adam Paquette"
    imageUri = "https://cards.scryfall.io/normal/front/8/e/8e9ac507-7c8f-431f-8d1a-220ceeacf871.jpg?1722282055"
    inBooster = false
}

/**
 * All Outlaws of Thunder Junction basic land variants.
 */
val OutlawsOfThunderJunctionBasicLands = listOf(
    OtjPlains272, OtjPlains277, OtjPlains278,
    OtjIsland273, OtjIsland279, OtjIsland280,
    OtjSwamp274, OtjSwamp281, OtjSwamp282,
    OtjMountain275, OtjMountain283, OtjMountain284,
    OtjForest276, OtjForest285, OtjForest286
)
