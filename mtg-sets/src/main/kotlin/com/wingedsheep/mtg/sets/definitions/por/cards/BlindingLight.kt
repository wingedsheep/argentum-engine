// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget


/**
 * Blinding Light
 * {2}{W}
 * Sorcery
 * Tap all nonwhite creatures.
 */
val BlindingLight = card("Blinding Light") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    spell {
        effect = ForEachInGroupEffect(GroupFilter(GameObjectFilter.Creature.notColor(Color.WHITE)), Effects.Tap(EffectTarget.Self))
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "John Coulthart"
        flavorText = "Let the unjust avert their faces and contemplate their peril."
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4ea283d2-8f00-4836-81b4-c041b0469dcb.jpg"
    }
}
