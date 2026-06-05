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
 * Bee Sting
 * {3}{G}
 * Sorcery
 * Bee Sting deals 2 damage to any target.
 */
val BeeSting = card("Bee Sting") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    spell {
        val t = target("target", AnyTarget())
        effect = DealDamageEffect(2, t)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Phil Foglio"
        flavorText = "There are few things as motivating as a swarm of bees."
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23bcf64a-ae3d-4abb-acc7-81bba237f37b.jpg"
    }
}
