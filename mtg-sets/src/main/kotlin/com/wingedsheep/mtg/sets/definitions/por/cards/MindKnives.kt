// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetOpponent


/**
 * Mind Knives
 * {1}{B}
 * Sorcery
 * Target opponent discards a card at random.
 */
val MindKnives = card("Mind Knives") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetOpponent())
        effect = EffectPatterns.discardRandom(1, t)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Rebecca Guay"
        flavorText = "To dodge the unerring knife, one must be free of thought."
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d17e23a6-6313-416d-b826-5df5833371dc.jpg"
    }
}
