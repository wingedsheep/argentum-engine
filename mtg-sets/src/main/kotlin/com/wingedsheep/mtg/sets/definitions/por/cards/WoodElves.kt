// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination


/**
 * Wood Elves
 * {2}{G}
 * Creature — Elf Scout
 * 1/1
 * When this creature enters, search your library for a Forest card, put that card onto the battlefield, then shuffle.
 */
val WoodElves = card("Wood Elves") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Scout"
    power = 1
    toughness = 1
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.searchLibrary(filter = GameObjectFilter.Land.withSubtype("Forest"), destination = SearchDestination.BATTLEFIELD)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "195"
        artist = "Rebecca Guay"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7f1fb90-5c85-46a5-802d-248cc0250921.jpg"
    }
}
