// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Temporary Truce
 * {1}{W}
 * Sorcery
 * Each player may draw up to two cards. For each card less than two a player draws this way, that player gains 2 life.
 */
val TemporaryTruce = card("Temporary Truce") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    spell {
        effect = EffectPatterns.eachPlayerMayDraw(maxCards = 2, lifePerCardNotDrawn = 2)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Mike Raabe"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f6ee294-7dbb-4872-81d1-c69c7337cf9f.jpg"
    }
}
