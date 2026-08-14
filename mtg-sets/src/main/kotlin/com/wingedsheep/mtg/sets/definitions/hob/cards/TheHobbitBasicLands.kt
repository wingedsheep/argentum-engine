package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.basicLand

val HobPlains = basicLand("Plains") {
    collectorNumber = "189"
    artist = "Chris Cold"
    imageUri = "https://cards.scryfall.io/normal/front/7/b/7b7c408b-8660-4db5-9a16-5003c11b4ac1.jpg"
}

val HobIsland = basicLand("Island") {
    collectorNumber = "190"
    artist = "Kamila Szutenberg"
    imageUri = "https://cards.scryfall.io/normal/front/c/6/c6aa89a8-3584-4906-b9a9-41ef2f021f8e.jpg"
}

val HobSwamp = basicLand("Swamp") {
    collectorNumber = "191"
    artist = "Erikas Perl"
    imageUri = "https://cards.scryfall.io/normal/front/4/0/4031e5e4-e573-4130-8d20-4a606edef0a0.jpg"
}

val HobMountain = basicLand("Mountain") {
    collectorNumber = "192"
    artist = "Shahab Alizadeh"
    imageUri = "https://cards.scryfall.io/normal/front/c/4/c49d378e-9549-4320-b3c6-1aeb216d1e98.jpg"
}

val HobForest = basicLand("Forest") {
    collectorNumber = "193"
    artist = "Kamila Szutenberg"
    imageUri = "https://cards.scryfall.io/normal/front/c/3/c3e84b42-5423-4d4d-b8fc-cfbb2c53a4ca.jpg"
}

val TheHobbitBasicLands = listOf(HobPlains, HobIsland, HobSwamp, HobMountain, HobForest)
