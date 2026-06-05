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
 * Boiling Seas
 * {3}{R}
 * Sorcery
 * Destroy all Islands.
 */
val BoilingSeas = card("Boiling Seas") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    spell {
        effect = ForEachInGroupEffect(GroupFilter(GameObjectFilter.Land.withSubtype("Island")), MoveToZoneEffect(EffectTarget.Self, Zone.GRAVEYARD, byDestruction = true), noRegenerate = false)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "Tom Wänerstrand"
        flavorText = "What burns the land, boils the seas."
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1523c1b-2ba1-4581-8502-47544d450d8e.jpg"
    }
}
