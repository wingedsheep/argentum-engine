// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.TauntEffect
import com.wingedsheep.sdk.scripting.targets.TargetPlayer


/**
 * Taunt
 * {U}
 * Sorcery
 * During target player's next turn, creatures that player controls attack you if able.
 */
val Taunt = card("Taunt") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetPlayer())
        effect = TauntEffect(t)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Phil Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4d87322-aba4-4187-9655-1da1f18615f8.jpg"
    }
}
