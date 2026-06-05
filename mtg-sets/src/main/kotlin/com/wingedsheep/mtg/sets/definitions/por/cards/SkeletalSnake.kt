// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Skeletal Snake
 * {1}{B}
 * Creature — Snake Skeleton
 * 2/1
 */
val SkeletalSnake = card("Skeletal Snake") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Snake Skeleton"
    power = 2
    toughness = 1
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "John Matson"
        flavorText = "They watched as the snake shed layer after layer until only cold bone remained."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42bd4896-4191-4479-be57-070753f8725c.jpg"
    }
}
