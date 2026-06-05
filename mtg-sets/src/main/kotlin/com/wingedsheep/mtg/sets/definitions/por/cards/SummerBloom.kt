// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PlayAdditionalLandsEffect


/**
 * Summer Bloom
 * {1}{G}
 * Sorcery
 * You may play up to three additional lands this turn.
 */
val SummerBloom = card("Summer Bloom") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    spell {
        effect = PlayAdditionalLandsEffect(3)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "187"
        artist = "Kaja Foglio"
        flavorText = "Summer sends its kiss with warmth and blooming life."
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e86abcc-272e-4959-90ee-343b9f546ea7.jpg"
    }
}
