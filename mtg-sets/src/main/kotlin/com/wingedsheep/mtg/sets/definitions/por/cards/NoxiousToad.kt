// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Noxious Toad
 * {2}{B}
 * Creature — Frog
 * 1/1
 * When this creature dies, each opponent discards a card.
 */
val NoxiousToad = card("Noxious Toad") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Frog"
    power = 1
    toughness = 1
    triggeredAbility {
        trigger = Triggers.Dies
        effect = EffectPatterns.eachOpponentDiscards(1)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "104"
        artist = "Adrian Smith"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5ec75ba-bae2-4ccc-b18b-ad4639cfb548.jpg"
    }
}
