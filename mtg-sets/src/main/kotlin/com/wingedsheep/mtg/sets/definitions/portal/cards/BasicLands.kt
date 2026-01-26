package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.basicLand

/**
 * Portal Basic Lands
 *
 * Portal contains 4 art variants of each basic land type.
 * Cards 196-215 (Plains 196-199, Island 200-203, Swamp 204-207, Mountain 208-211, Forest 212-215)
 */

// =============================================================================
// Plains (Cards 196-199)
// =============================================================================

val Plains196 = basicLand("Plains") {
    collectorNumber = "196"
    artist = "Douglas Shuler"
    imageUri = "https://cards.scryfall.io/normal/front/9/0/90d35453-7fe3-4053-aad9-a124ecc7dcf0.jpg"
}

val Plains197 = basicLand("Plains") {
    collectorNumber = "197"
    artist = "Douglas Shuler"
    imageUri = "https://cards.scryfall.io/normal/front/a/1/a1de52fb-796c-4146-b592-00ce0cc5a187.jpg"
}

val Plains198 = basicLand("Plains") {
    collectorNumber = "198"
    artist = "Douglas Shuler"
    imageUri = "https://cards.scryfall.io/normal/front/3/a/3a48c07b-ddbe-4251-b9e7-38ea60d66e72.jpg"
}

val Plains199 = basicLand("Plains") {
    collectorNumber = "199"
    artist = "Douglas Shuler"
    imageUri = "https://cards.scryfall.io/normal/front/3/e/3e511581-2739-445e-b486-c2b2d15c9be4.jpg"
}

// =============================================================================
// Island (Cards 200-203)
// =============================================================================

val Island200 = basicLand("Island") {
    collectorNumber = "200"
    artist = "Eric Peterson"
    imageUri = "https://cards.scryfall.io/normal/front/e/9/e98d1e6f-5902-4e67-91a6-30eb5c3ce4a1.jpg"
}

val Island201 = basicLand("Island") {
    collectorNumber = "201"
    artist = "Eric Peterson"
    imageUri = "https://cards.scryfall.io/normal/front/d/7/d7fcc1eb-8689-4b0f-9e56-cfeb4c8c1edb.jpg"
}

val Island202 = basicLand("Island") {
    collectorNumber = "202"
    artist = "Eric Peterson"
    imageUri = "https://cards.scryfall.io/normal/front/3/6/36da118a-d54d-489d-85da-5cbcd0d80519.jpg"
}

val Island203 = basicLand("Island") {
    collectorNumber = "203"
    artist = "Eric Peterson"
    imageUri = "https://cards.scryfall.io/normal/front/f/5/f591c9c1-ce72-4b68-b32f-4d048dcfa495.jpg"
}

// =============================================================================
// Swamp (Cards 204-207)
// =============================================================================

val Swamp204 = basicLand("Swamp") {
    collectorNumber = "204"
    artist = "Romas Kukalis"
    imageUri = "https://cards.scryfall.io/normal/front/e/c/ec0da69e-4ab6-4ef1-a7ae-4d6c47172c81.jpg"
}

val Swamp205 = basicLand("Swamp") {
    collectorNumber = "205"
    artist = "Romas Kukalis"
    imageUri = "https://cards.scryfall.io/normal/front/d/d/dd6c1c8d-dd83-4dbe-b022-495edd1a909f.jpg"
}

val Swamp206 = basicLand("Swamp") {
    collectorNumber = "206"
    artist = "Romas Kukalis"
    imageUri = "https://cards.scryfall.io/normal/front/2/c/2c0c5cf6-f91d-45eb-a5c1-5aacec9cda69.jpg"
}

val Swamp207 = basicLand("Swamp") {
    collectorNumber = "207"
    artist = "Romas Kukalis"
    imageUri = "https://cards.scryfall.io/normal/front/5/f/5fd26e01-cbd2-4787-bc01-aff58c13840f.jpg"
}

// =============================================================================
// Mountain (Cards 208-211)
// =============================================================================

val Mountain208 = basicLand("Mountain") {
    collectorNumber = "208"
    artist = "Brian Durfee"
    imageUri = "https://cards.scryfall.io/normal/front/1/7/17cf7ce4-d5d7-49f2-a7e4-021d1a2d58c5.jpg"
}

val Mountain209 = basicLand("Mountain") {
    collectorNumber = "209"
    artist = "Brian Durfee"
    imageUri = "https://cards.scryfall.io/normal/front/b/8/b86914cf-0808-4c7c-b364-1539087688bc.jpg"
}

val Mountain210 = basicLand("Mountain") {
    collectorNumber = "210"
    artist = "Brian Durfee"
    imageUri = "https://cards.scryfall.io/normal/front/c/e/ce16a4aa-7476-4999-ad1f-5d8bc27bf418.jpg"
}

val Mountain211 = basicLand("Mountain") {
    collectorNumber = "211"
    artist = "Brian Durfee"
    imageUri = "https://cards.scryfall.io/normal/front/2/6/26c5f3d2-6728-47b9-8178-a640e23ba30a.jpg"
}

// =============================================================================
// Forest (Cards 212-215)
// =============================================================================

val Forest212 = basicLand("Forest") {
    collectorNumber = "212"
    artist = "John Avon"
    imageUri = "https://cards.scryfall.io/normal/front/4/0/40146f61-d3f0-45e7-82b5-788ff7b0e520.jpg"
}

val Forest213 = basicLand("Forest") {
    collectorNumber = "213"
    artist = "John Avon"
    imageUri = "https://cards.scryfall.io/normal/front/4/4/448f15f2-abfa-4176-856f-be815152b620.jpg"
}

val Forest214 = basicLand("Forest") {
    collectorNumber = "214"
    artist = "John Avon"
    imageUri = "https://cards.scryfall.io/normal/front/b/3/b37ea002-f81e-4fa6-ac24-bba1cb6d7edf.jpg"
}

val Forest215 = basicLand("Forest") {
    collectorNumber = "215"
    artist = "John Avon"
    imageUri = "https://cards.scryfall.io/normal/front/b/4/b44a98ba-d999-41b7-a5c6-f3efa6056800.jpg"
}

/**
 * All Portal basic land variants.
 */
val PortalBasicLands = listOf(
    Plains196, Plains197, Plains198, Plains199,
    Island200, Island201, Island202, Island203,
    Swamp204, Swamp205, Swamp206, Swamp207,
    Mountain208, Mountain209, Mountain210, Mountain211,
    Forest212, Forest213, Forest214, Forest215
)
