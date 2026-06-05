// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetPlayer


/**
 * Mind Rot
 * {2}{B}
 * Sorcery
 * Target player discards two cards.
 */
val MindRot = card("Mind Rot") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetPlayer())
        effect = EffectPatterns.discardCards(2, t)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Steve Luke"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b91d355d-8409-4f0b-87ce-7590a8b9ebc0.jpg"
    }
}
