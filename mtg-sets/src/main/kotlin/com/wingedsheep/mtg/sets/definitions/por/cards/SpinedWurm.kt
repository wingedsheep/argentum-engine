// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Spined Wurm
 * {4}{G}
 * Creature — Wurm
 * 5/4
 */
val SpinedWurm = card("Spined Wurm") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wurm"
    power = 5
    toughness = 4
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "185"
        artist = "Colin MacNeil"
        flavorText = "It has more teeth than fit in its mouth."
        imageUri = "https://cards.scryfall.io/normal/front/0/0/0053bd00-90fd-48c2-8f79-952d5d1e3e74.jpg"
    }
}
