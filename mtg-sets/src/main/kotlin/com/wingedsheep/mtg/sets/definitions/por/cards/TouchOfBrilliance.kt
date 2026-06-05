// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect


/**
 * Touch of Brilliance
 * {3}{U}
 * Sorcery
 * Draw two cards.
 */
val TouchofBrilliance = card("Touch of Brilliance") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    spell {
        effect = DrawCardsEffect(2)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "John Coulthart"
        flavorText = "Acting on one good idea is better than hoarding all the world's knowledge."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/196474ce-e28e-48f0-b407-dc5535adf1b6.jpg"
    }
}
