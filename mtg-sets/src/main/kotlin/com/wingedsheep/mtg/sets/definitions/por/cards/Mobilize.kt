// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget


/**
 * Mobilize
 * {G}
 * Sorcery
 * Untap all creatures you control.
 */
val Mobilize = card("Mobilize") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    spell {
        effect = ForEachInGroupEffect(GroupFilter(GameObjectFilter.Creature.youControl()), Effects.Untap(EffectTarget.Self))
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "172"
        artist = "Rebecca Guay"
        flavorText = "A rested mind is the sharpest weapon."
        imageUri = "https://cards.scryfall.io/normal/front/9/7/9712ecaa-4059-44ba-98b7-07bfe7411b5b.jpg"
    }
}
