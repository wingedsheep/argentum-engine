// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent


/**
 * Winter's Grasp
 * {1}{G}{G}
 * Sorcery
 * Destroy target land.
 */
val WintersGrasp = card("Winter's Grasp") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetPermanent(filter = TargetFilter.Land))
        effect = MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "194"
        artist = "Paolo Parente"
        flavorText = "Winter settles on the land, and the land prays it will wake."
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2215de4-da49-4270-aec7-5e16a938bae4.jpg"
    }
}
