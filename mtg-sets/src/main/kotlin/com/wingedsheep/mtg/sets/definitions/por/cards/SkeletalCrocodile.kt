// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Skeletal Crocodile
 * {3}{B}
 * Creature — Crocodile Skeleton
 * 5/1
 */
val SkeletalCrocodile = card("Skeletal Crocodile") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Crocodile Skeleton"
    power = 5
    toughness = 1
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Mike Dringenberg"
        flavorText = "The less flesh there is, the more teeth there seem to be."
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebcbbd6f-2915-4b4c-85d3-1d9b55d36c11.jpg"
    }
}
