package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.basicLand

/**
 * Wilds of Eldraine Basic Lands
 *
 * Three art variants of each basic land type: the full-art paper-cut cycle by Hari & Deepti
 * (cards 262-266) plus two regular-frame arts per type (cards 267-276). All fifteen are booster
 * printings, so all are available in the limited basic-land pool.
 */

// =============================================================================
// Plains (Cards 262, 267, 268)
// =============================================================================

val WoePlains262 = basicLand("Plains") {
    collectorNumber = "262"
    artist = "Hari & Deepti"
    imageUri = "https://cards.scryfall.io/normal/front/c/9/c9cd4d57-8c51-4fcf-8a9f-5d6a61c33e3d.jpg?1783915053"
}

val WoePlains267 = basicLand("Plains") {
    collectorNumber = "267"
    artist = "Carlos Palma Cruchaga"
    imageUri = "https://cards.scryfall.io/normal/front/e/e/eed1af19-e075-4e6f-9394-d258143e15a4.jpg?1783915052"
}

val WoePlains268 = basicLand("Plains") {
    collectorNumber = "268"
    artist = "Jonas De Ro"
    imageUri = "https://cards.scryfall.io/normal/front/4/8/486fbcf9-3a04-47f6-8927-886c2a454499.jpg?1783915051"
}

// =============================================================================
// Island (Cards 263, 269, 270)
// =============================================================================

val WoeIsland263 = basicLand("Island") {
    collectorNumber = "263"
    artist = "Hari & Deepti"
    imageUri = "https://cards.scryfall.io/normal/front/b/d/bd4b4da4-83f6-4280-880b-b6033308f2a2.jpg?1783915053"
}

val WoeIsland269 = basicLand("Island") {
    collectorNumber = "269"
    artist = "Leanna Crossan"
    imageUri = "https://cards.scryfall.io/normal/front/6/2/6245d2f8-8ef0-4d2a-9abb-99839dc3abf0.jpg?1783915051"
}

val WoeIsland270 = basicLand("Island") {
    collectorNumber = "270"
    artist = "Sarah Finnigan"
    imageUri = "https://cards.scryfall.io/normal/front/1/a/1a9798d6-34b3-4438-992d-d3616a7c8536.jpg?1783915051"
}

// =============================================================================
// Swamp (Cards 264, 271, 272)
// =============================================================================

val WoeSwamp264 = basicLand("Swamp") {
    collectorNumber = "264"
    artist = "Hari & Deepti"
    imageUri = "https://cards.scryfall.io/normal/front/e/e/ee68f2cb-851b-4196-ac58-844d72628e6a.jpg?1783915052"
}

val WoeSwamp271 = basicLand("Swamp") {
    collectorNumber = "271"
    artist = "Jonas De Ro"
    imageUri = "https://cards.scryfall.io/normal/front/d/0/d0a801ba-ebf7-4b9e-98c2-db50448845b7.jpg?1783915052"
}

val WoeSwamp272 = basicLand("Swamp") {
    collectorNumber = "272"
    artist = "Julian Kok Joon Wen"
    imageUri = "https://cards.scryfall.io/normal/front/f/0/f0de4ea5-77e6-474e-99f9-36192bbd37d5.jpg?1783915051"
}

// =============================================================================
// Mountain (Cards 265, 273, 274)
// =============================================================================

val WoeMountain265 = basicLand("Mountain") {
    collectorNumber = "265"
    artist = "Hari & Deepti"
    imageUri = "https://cards.scryfall.io/normal/front/8/8/8822db23-34dc-452a-92bc-a3ceee4db375.jpg?1783915052"
}

val WoeMountain273 = basicLand("Mountain") {
    collectorNumber = "273"
    artist = "Sarah Finnigan"
    imageUri = "https://cards.scryfall.io/normal/front/8/9/8996cd43-5ecd-4a05-a5a9-e49326befaa1.jpg?1783915051"
}

val WoeMountain274 = basicLand("Mountain") {
    collectorNumber = "274"
    artist = "Julian Kok Joon Wen"
    imageUri = "https://cards.scryfall.io/normal/front/8/e/8e747bba-b521-4f81-9d9a-3e85747cefe9.jpg?1783915051"
}

// =============================================================================
// Forest (Cards 266, 275, 276)
// =============================================================================

val WoeForest266 = basicLand("Forest") {
    collectorNumber = "266"
    artist = "Hari & Deepti"
    imageUri = "https://cards.scryfall.io/normal/front/e/c/ecd6d8fb-780c-446c-a8bf-93386b22fe95.jpg?1783915051"
}

val WoeForest275 = basicLand("Forest") {
    collectorNumber = "275"
    artist = "Jonas De Ro"
    imageUri = "https://cards.scryfall.io/normal/front/a/4/a4c99f1b-f304-42d5-bbea-32283b01d43b.jpg?1783915048"
}

val WoeForest276 = basicLand("Forest") {
    collectorNumber = "276"
    artist = "Adam Paquette"
    imageUri = "https://cards.scryfall.io/normal/front/1/b/1bd51a22-3e0d-4826-aab7-0adbfce4478a.jpg?1783915049"
}

/**
 * All Wilds of Eldraine basic land variants.
 */
val WildsOfEldraineBasicLands = listOf(
    WoePlains262, WoePlains267, WoePlains268,
    WoeIsland263, WoeIsland269, WoeIsland270,
    WoeSwamp264, WoeSwamp271, WoeSwamp272,
    WoeMountain265, WoeMountain273, WoeMountain274,
    WoeForest266, WoeForest275, WoeForest276,
)
