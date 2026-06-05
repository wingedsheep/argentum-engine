// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature


/**
 * Forked Lightning
 * {3}{R}
 * Sorcery
 * Forked Lightning deals 4 damage divided as you choose among one, two, or three target creatures.
 */
val ForkedLightning = card("Forked Lightning") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    spell {
        target = TargetCreature(count = 3, minCount = 1)
        effect = DividedDamageEffect(totalDamage = 4, minTargets = 1, maxTargets = 3)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "130"
        artist = "Ted Naifeh"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85883197-fd55-4e82-862f-87be4f789493.jpg"
    }
}
