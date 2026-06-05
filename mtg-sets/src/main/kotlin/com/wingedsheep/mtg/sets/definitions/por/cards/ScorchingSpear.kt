// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.AnyTarget


/**
 * Scorching Spear
 * {R}
 * Sorcery
 * Scorching Spear deals 1 damage to any target.
 */
val ScorchingSpear = card("Scorching Spear") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    spell {
        val t = target("target", AnyTarget())
        effect = DealDamageEffect(1, t)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "Mike Raabe"
        flavorText = "Lift your spear as you might lift your glass, and toast your enemy."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e4817bd-68e8-4a85-983a-ee6dda2bbf33.jpg"
    }
}
