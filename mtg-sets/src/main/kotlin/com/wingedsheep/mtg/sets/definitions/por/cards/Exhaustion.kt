// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SkipUntapEffect
import com.wingedsheep.sdk.scripting.targets.TargetOpponent


/**
 * Exhaustion
 * {2}{U}
 * Sorcery
 * Creatures and lands target opponent controls don't untap during their next untap step.
 */
val Exhaustion = card("Exhaustion") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetOpponent())
        effect = SkipUntapEffect(t)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "DiTerlizzi"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d6a5c33-cf74-4cec-a4f4-1aac9e7b8f79.jpg"
    }
}
