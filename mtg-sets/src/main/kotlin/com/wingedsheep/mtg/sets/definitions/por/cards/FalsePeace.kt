// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SkipCombatPhasesEffect
import com.wingedsheep.sdk.scripting.targets.TargetPlayer


/**
 * False Peace
 * {W}
 * Sorcery
 * Target player skips all combat phases of their next turn.
 */
val FalsePeace = card("False Peace") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetPlayer())
        effect = SkipCombatPhasesEffect(t)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "Zina Saunders"
        flavorText = "Mutual consent is not required for war."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4234262-56c6-4bd1-b425-12db931829d5.jpg"
    }
}
