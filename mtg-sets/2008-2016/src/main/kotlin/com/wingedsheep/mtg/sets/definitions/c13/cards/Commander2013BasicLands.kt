package com.wingedsheep.mtg.sets.definitions.c13.cards

import com.wingedsheep.sdk.dsl.basicLand

/**
 * Commander 2013 Basic Lands
 *
 * C13 prints a single art for each basic land type (337, 341, 345, 349, 353).
 */

val Commander2013Plains = basicLand("Plains") {
    collectorNumber = "337"
    artist = "John Avon"
    imageUri = "https://cards.scryfall.io/normal/front/5/0/503dfa54-a99e-40bc-a809-f1b7cb410eab.jpg"
}

val Commander2013Island = basicLand("Island") {
    collectorNumber = "341"
    artist = "Noah Bradley"
    imageUri = "https://cards.scryfall.io/normal/front/c/a/caf4bd23-7fcd-47a9-9fc0-0f251299d7d0.jpg"
}

val Commander2013Swamp = basicLand("Swamp") {
    collectorNumber = "345"
    artist = "Mike Bierek"
    imageUri = "https://cards.scryfall.io/normal/front/0/e/0e02d146-925e-47b4-ae9a-8e5fc542ea08.jpg"
}

val Commander2013Mountain = basicLand("Mountain") {
    collectorNumber = "349"
    artist = "Cliff Childs"
    imageUri = "https://cards.scryfall.io/normal/front/d/a/daeda166-5add-4af1-bb6d-43ce5d754074.jpg"
}

val Commander2013Forest = basicLand("Forest") {
    collectorNumber = "353"
    artist = "Rob Alexander"
    imageUri = "https://cards.scryfall.io/normal/front/e/0/e005bfcd-5c99-4d92-99d9-4ba97b52af77.jpg"
}

/**
 * All Commander 2013 basic land variants.
 */
val Commander2013BasicLands = listOf(
    Commander2013Plains,
    Commander2013Island,
    Commander2013Swamp,
    Commander2013Mountain,
    Commander2013Forest,
)
