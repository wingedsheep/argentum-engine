package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.dsl.basicLand

/**
 * Limited Edition Beta Basic Lands
 *
 * Beta printed three art variants of each basic land type: Plains 288-290, Island 291-293,
 * Swamp 294-296, Mountain 297-299, Forest 300-302.
 */

// =============================================================================
// Plains (Cards 288-290)
// =============================================================================

val LebPlains288 = basicLand("Plains") {
    collectorNumber = "288"
    artist = "Jesper Myrfors"
    imageUri = "https://cards.scryfall.io/normal/front/b/7/b7331b03-be66-419c-94bc-ed494c042ea3.jpg?1783948595"
}

val LebPlains289 = basicLand("Plains") {
    collectorNumber = "289"
    artist = "Jesper Myrfors"
    imageUri = "https://cards.scryfall.io/normal/front/5/2/52ff493a-6336-416e-af5e-1eb6d10c080e.jpg?1783948595"
}

val LebPlains290 = basicLand("Plains") {
    collectorNumber = "290"
    artist = "Jesper Myrfors"
    imageUri = "https://cards.scryfall.io/normal/front/3/8/38e2b0ff-8fdf-4db0-85c0-c1010bacd36b.jpg?1783948595"
}

// =============================================================================
// Island (Cards 291-293)
// =============================================================================

val LebIsland291 = basicLand("Island") {
    collectorNumber = "291"
    artist = "Mark Poole"
    imageUri = "https://cards.scryfall.io/normal/front/b/f/bff33e91-8e52-43f2-b8ae-603b456b08fc.jpg?1783948594"
}

val LebIsland292 = basicLand("Island") {
    collectorNumber = "292"
    artist = "Mark Poole"
    imageUri = "https://cards.scryfall.io/normal/front/d/0/d0c5cf64-9844-4b5b-8e6b-b97c50cce053.jpg?1783948594"
}

val LebIsland293 = basicLand("Island") {
    collectorNumber = "293"
    artist = "Mark Poole"
    imageUri = "https://cards.scryfall.io/normal/front/c/0/c0a612c4-b4ac-4dd2-a06e-92516599fafd.jpg?1783948594"
}

// =============================================================================
// Swamp (Cards 294-296)
// =============================================================================

val LebSwamp294 = basicLand("Swamp") {
    collectorNumber = "294"
    artist = "Dan Frazier"
    imageUri = "https://cards.scryfall.io/normal/front/d/1/d1309a80-a761-4b80-8cf1-1a8b83190511.jpg?1783948594"
}

val LebSwamp295 = basicLand("Swamp") {
    collectorNumber = "295"
    artist = "Dan Frazier"
    imageUri = "https://cards.scryfall.io/normal/front/2/5/25ad2444-9985-423c-ad36-387218866409.jpg?1783948594"
}

val LebSwamp296 = basicLand("Swamp") {
    collectorNumber = "296"
    artist = "Dan Frazier"
    imageUri = "https://cards.scryfall.io/normal/front/a/3/a3544148-49b2-4320-8e3a-5bab81e0f7fd.jpg?1783948594"
}

// =============================================================================
// Mountain (Cards 297-299)
// =============================================================================

val LebMountain297 = basicLand("Mountain") {
    collectorNumber = "297"
    artist = "Douglas Shuler"
    imageUri = "https://cards.scryfall.io/normal/front/7/a/7af9c715-8d72-4eae-b412-fc89138ff588.jpg?1783948594"
}

val LebMountain298 = basicLand("Mountain") {
    collectorNumber = "298"
    artist = "Douglas Shuler"
    imageUri = "https://cards.scryfall.io/normal/front/7/c/7cb88a03-7092-4d31-a9f1-4f16e39bc537.jpg?1783948593"
}

val LebMountain299 = basicLand("Mountain") {
    collectorNumber = "299"
    artist = "Douglas Shuler"
    imageUri = "https://cards.scryfall.io/normal/front/a/f/af9ad645-e605-4048-bf4c-d636584f315b.jpg?1783948593"
}

// =============================================================================
// Forest (Cards 300-302)
// =============================================================================

val LebForest300 = basicLand("Forest") {
    collectorNumber = "300"
    artist = "Christopher Rush"
    imageUri = "https://cards.scryfall.io/normal/front/b/5/b5a922eb-49c7-45f0-92bc-671d7a8758f4.jpg?1783948592"
}

val LebForest301 = basicLand("Forest") {
    collectorNumber = "301"
    artist = "Christopher Rush"
    imageUri = "https://cards.scryfall.io/normal/front/8/9/89ad91fc-50c2-44e0-b88e-2c13610377f9.jpg?1783948592"
}

val LebForest302 = basicLand("Forest") {
    collectorNumber = "302"
    artist = "Christopher Rush"
    imageUri = "https://cards.scryfall.io/normal/front/b/4/b4075bbc-dbad-4a1e-a992-70aed713a459.jpg?1783948591"
}

val LimitedEditionBetaBasicLands = listOf(
    LebPlains288,
    LebPlains289,
    LebPlains290,
    LebIsland291,
    LebIsland292,
    LebIsland293,
    LebSwamp294,
    LebSwamp295,
    LebSwamp296,
    LebMountain297,
    LebMountain298,
    LebMountain299,
    LebForest300,
    LebForest301,
    LebForest302,
)
