// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget


/**
 * Devastation
 * {5}{R}{R}
 * Sorcery
 * Destroy all creatures and lands.
 */
val Devastation = card("Devastation") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    spell {
        effect = ForEachInGroupEffect(GroupFilter(GameObjectFilter.CreatureOrLand), MoveToZoneEffect(EffectTarget.Self, Zone.GRAVEYARD, byDestruction = true), noRegenerate = false)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "123"
        artist = "Steve Luke"
        flavorText = "There is much talk about the art of creation. What about the art of destruction?"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71cce019-162c-4969-89ac-1cf94148a032.jpg"
    }
}
