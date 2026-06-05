// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.TargetPlayerOrPlaneswalker


/**
 * Lava Axe
 * {4}{R}
 * Sorcery
 * Lava Axe deals 5 damage to target player or planeswalker.
 */
val LavaAxe = card("Lava Axe") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetPlayerOrPlaneswalker())
        effect = DealDamageEffect(5, t)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "137"
        artist = "Adrian Smith"
        flavorText = "Swing your axe as a broom, to sweep away the foe."
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2bebbad-76aa-4388-891a-583e8af9509d.jpg"
    }
}
