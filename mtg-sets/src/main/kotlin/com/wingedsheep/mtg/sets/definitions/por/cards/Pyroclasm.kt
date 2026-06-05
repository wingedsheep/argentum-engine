// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget


/**
 * Pyroclasm
 * {1}{R}
 * Sorcery
 * Pyroclasm deals 2 damage to each creature.
 */
val Pyroclasm = card("Pyroclasm") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    spell {
        effect = ForEachInGroupEffect(GroupFilter(GameObjectFilter.Creature), DealDamageEffect(2, EffectTarget.Self))
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "143"
        artist = "John Matson"
        flavorText = "Chaos does not choose its enemies."
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de214247-e5e3-4d8f-935a-797218416be1.jpg"
    }
}
