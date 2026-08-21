package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.basicLand

/**
 * Modern Horizons 2 Basic Lands
 *
 * MH2 prints two art variants of each basic land type (cards 481-490), each a retro-frame reprint
 * of a classic piece: Plains 481-482, Island 483-484, Swamp 485-486, Mountain 487-488,
 * Forest 489-490.
 */

// Plains (481-482)
val Mh2Plains481 = basicLand("Plains") {
    collectorNumber = "481"
    artist = "Eric Peterson"
    imageUri = "https://cards.scryfall.io/normal/front/d/5/d5aa2449-6b74-4319-858f-caa9282da5c1.jpg?1783926700"
}
val Mh2Plains482 = basicLand("Plains") {
    collectorNumber = "482"
    artist = "Alan Pollack"
    imageUri = "https://cards.scryfall.io/normal/front/e/1/e14e6193-f60b-46c8-a7eb-02a437138568.jpg?1783926700"
}

// Island (483-484)
val Mh2Island483 = basicLand("Island") {
    collectorNumber = "483"
    artist = "Donato Giancola"
    imageUri = "https://cards.scryfall.io/normal/front/4/2/423927e6-2419-4d88-b0c4-425e5cac1a3f.jpg?1783926698"
}
val Mh2Island484 = basicLand("Island") {
    collectorNumber = "484"
    artist = "Douglas Shuler"
    imageUri = "https://cards.scryfall.io/normal/front/9/5/95d781a5-2f5e-499f-b210-c4a5c50a180c.jpg?1783926702"
}

// Swamp (485-486)
val Mh2Swamp485 = basicLand("Swamp") {
    collectorNumber = "485"
    artist = "Jerry Tiritilli"
    imageUri = "https://cards.scryfall.io/normal/front/a/c/ac8546d1-bfea-4cf7-bfa1-48555ea81bd4.jpg?1783926697"
}
val Mh2Swamp486 = basicLand("Swamp") {
    collectorNumber = "486"
    artist = "Pete Venters"
    imageUri = "https://cards.scryfall.io/normal/front/9/5/959cb185-a280-493c-b85c-69b74e042c15.jpg?1783926697"
}

// Mountain (487-488)
val Mh2Mountain487 = basicLand("Mountain") {
    collectorNumber = "487"
    artist = "Heather Hudson"
    imageUri = "https://cards.scryfall.io/normal/front/a/4/a4be3032-c55b-43b5-9ae0-f4e7470f4f83.jpg?1783926696"
}
val Mh2Mountain488 = basicLand("Mountain") {
    collectorNumber = "488"
    artist = "Tony Szczudlo"
    imageUri = "https://cards.scryfall.io/normal/front/0/f/0f5a0d49-71ae-42c4-a896-6828dc4f1e85.jpg?1783926694"
}

// Forest (489-490)
val Mh2Forest489 = basicLand("Forest") {
    collectorNumber = "489"
    artist = "Rob Alexander"
    imageUri = "https://cards.scryfall.io/normal/front/4/6/46e93212-da68-48f8-9aeb-ee5eb92e9a54.jpg?1783926697"
}
val Mh2Forest490 = basicLand("Forest") {
    collectorNumber = "490"
    artist = "John Avon"
    imageUri = "https://cards.scryfall.io/normal/front/1/7/17393110-c57e-487b-b07e-dc21a164efa7.jpg?1783926693"
}
