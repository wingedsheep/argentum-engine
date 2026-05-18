package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.basicLand

/**
 * Invasion Basic Lands
 *
 * Invasion contains 4 art variants of each basic land type (cards 331-350).
 * Mountain occupies 343-346.
 */

val InvMountain343 = basicLand("Mountain") {
    collectorNumber = "343"
    artist = "Matt Cavotta"
    imageUri = "https://cards.scryfall.io/normal/front/b/a/ba6694bb-f3b7-48ff-9d93-cbed84fac210.jpg?1562932524"
}

val InvMountain344 = basicLand("Mountain") {
    collectorNumber = "344"
    artist = "Jeff Miracola"
    imageUri = "https://cards.scryfall.io/normal/front/9/7/977527da-2953-493f-8e8c-ffc64ddeaf10.jpg?1562925536"
}

val InvMountain345 = basicLand("Mountain") {
    collectorNumber = "345"
    artist = "Glen Angus"
    imageUri = "https://cards.scryfall.io/normal/front/6/8/68df89dc-3909-4051-adc1-a86589d0e99d.jpg?1562916125"
}

val InvMountain346 = basicLand("Mountain") {
    collectorNumber = "346"
    artist = "Scott Bailey"
    imageUri = "https://cards.scryfall.io/normal/front/7/e/7e8ae541-98e2-4a84-90a6-b17502f4442d.jpg?1562920542"
}

val InvasionBasicLands = listOf(
    InvMountain343,
    InvMountain344,
    InvMountain345,
    InvMountain346,
)
