// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature


/**
 * Time Ebb
 * {2}{U}
 * Sorcery
 * Put target creature on top of its owner's library.
 */
val TimeEbb = card("Time Ebb") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = MoveToZoneEffect(t, Zone.LIBRARY, ZonePlacement.Top)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Alan Rabinowitz"
        flavorText = "Like the tide, time both ebbs and flows."
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5fd26ca-dc7d-453d-8653-7f967e8f6dc7.jpg"
    }
}
