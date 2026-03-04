package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.basicLand

/**
 * Khans of Tarkir Basic Lands
 *
 * Khans of Tarkir contains 4 art variants of each basic land type.
 * Cards 250-269 (Plains 250-253, Island 254-257, Swamp 258-261, Mountain 262-265, Forest 266-269)
 */

// =============================================================================
// Plains (Cards 250-253)
// =============================================================================

val Plains250 = basicLand("Plains") {
    collectorNumber = "250"
    artist = "Noah Bradley"
    imageUri = "https://cards.scryfall.io/normal/front/2/5/2576dc74-1847-44b8-aef9-9d74f333b9cc.jpg"
}

val Plains251 = basicLand("Plains") {
    collectorNumber = "251"
    artist = "Sam Burley"
    imageUri = "https://cards.scryfall.io/normal/front/4/0/40337e60-065e-425d-ae35-c639d2bb5b42.jpg"
}

val Plains252 = basicLand("Plains") {
    collectorNumber = "252"
    artist = "Sam Burley"
    imageUri = "https://cards.scryfall.io/normal/front/a/5/a5bfb654-ab99-4272-a184-b95e1022af5c.jpg"
}

val Plains253 = basicLand("Plains") {
    collectorNumber = "253"
    artist = "Florian de Gesincourt"
    imageUri = "https://cards.scryfall.io/normal/front/3/2/324b97fb-da4a-4867-abde-825383c74b63.jpg"
}

// =============================================================================
// Island (Cards 254-257)
// =============================================================================

val Island254 = basicLand("Island") {
    collectorNumber = "254"
    artist = "Florian de Gesincourt"
    imageUri = "https://cards.scryfall.io/normal/front/9/c/9c010ca5-b609-4ae9-8c23-adf5723a9daa.jpg"
}

val Island255 = basicLand("Island") {
    collectorNumber = "255"
    artist = "Florian de Gesincourt"
    imageUri = "https://cards.scryfall.io/normal/front/0/a/0ac65c53-7b35-4ba0-9344-b311eee087cd.jpg"
}

val Island256 = basicLand("Island") {
    collectorNumber = "256"
    artist = "Titus Lunter"
    imageUri = "https://cards.scryfall.io/normal/front/a/4/a45827e8-d4b9-4a42-8516-22560568e678.jpg"
}

val Island257 = basicLand("Island") {
    collectorNumber = "257"
    artist = "Adam Paquette"
    imageUri = "https://cards.scryfall.io/normal/front/a/6/a61c4c14-e45d-45a8-87c7-fec98d46ee79.jpg"
}

// =============================================================================
// Swamp (Cards 258-261)
// =============================================================================

val Swamp258 = basicLand("Swamp") {
    collectorNumber = "258"
    artist = "Noah Bradley"
    imageUri = "https://cards.scryfall.io/normal/front/0/9/09d7e861-8dda-4073-bfd6-4ade3bca5cff.jpg"
}

val Swamp259 = basicLand("Swamp") {
    collectorNumber = "259"
    artist = "Sam Burley"
    imageUri = "https://cards.scryfall.io/normal/front/f/2/f2a8d790-61e5-4b46-94b8-54ea68ed18ea.jpg"
}

val Swamp260 = basicLand("Swamp") {
    collectorNumber = "260"
    artist = "Adam Paquette"
    imageUri = "https://cards.scryfall.io/normal/front/c/d/cd11fd2a-d872-4abc-ac2b-c0678c1be773.jpg"
}

val Swamp261 = basicLand("Swamp") {
    collectorNumber = "261"
    artist = "Adam Paquette"
    imageUri = "https://cards.scryfall.io/normal/front/1/f/1f7cac8b-6609-42ac-a594-002068e02de4.jpg"
}

// =============================================================================
// Mountain (Cards 262-265)
// =============================================================================

val Mountain262 = basicLand("Mountain") {
    collectorNumber = "262"
    artist = "Noah Bradley"
    imageUri = "https://cards.scryfall.io/normal/front/8/0/802b1bb0-6c73-481a-ac3e-7d4e1682b4c2.jpg"
}

val Mountain263 = basicLand("Mountain") {
    collectorNumber = "263"
    artist = "Noah Bradley"
    imageUri = "https://cards.scryfall.io/normal/front/5/0/5037330a-6e9b-4ce5-ba81-ae1ccef63334.jpg"
}

val Mountain264 = basicLand("Mountain") {
    collectorNumber = "264"
    artist = "Florian de Gesincourt"
    imageUri = "https://cards.scryfall.io/normal/front/4/f/4f45c7ed-cf98-455b-b665-81103fdc9331.jpg"
}

val Mountain265 = basicLand("Mountain") {
    collectorNumber = "265"
    artist = "Titus Lunter"
    imageUri = "https://cards.scryfall.io/normal/front/7/6/76ad8df0-20f3-4372-8b2b-7c4ccfc2e9c0.jpg"
}

// =============================================================================
// Forest (Cards 266-269)
// =============================================================================

val Forest266 = basicLand("Forest") {
    collectorNumber = "266"
    artist = "Sam Burley"
    imageUri = "https://cards.scryfall.io/normal/front/3/e/3e5342f9-3f91-48e4-bfd1-49fbf9ca89cf.jpg"
}

val Forest267 = basicLand("Forest") {
    collectorNumber = "267"
    artist = "Titus Lunter"
    imageUri = "https://cards.scryfall.io/normal/front/7/2/722111d4-99f4-463b-b232-96821c18f189.jpg"
}

val Forest268 = basicLand("Forest") {
    collectorNumber = "268"
    artist = "Titus Lunter"
    imageUri = "https://cards.scryfall.io/normal/front/1/a/1abe7f25-71c5-4fd2-8696-0a4ce8c4b0b6.jpg"
}

val Forest269 = basicLand("Forest") {
    collectorNumber = "269"
    artist = "Adam Paquette"
    imageUri = "https://cards.scryfall.io/normal/front/b/7/b742ffa7-915f-45ce-b3f2-8391723bb34c.jpg"
}

/**
 * All Khans of Tarkir basic land variants.
 */
val KhansBasicLands = listOf(
    Plains250, Plains251, Plains252, Plains253,
    Island254, Island255, Island256, Island257,
    Swamp258, Swamp259, Swamp260, Swamp261,
    Mountain262, Mountain263, Mountain264, Mountain265,
    Forest266, Forest267, Forest268, Forest269
)
