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
 * Rain of Tears
 * {1}{B}{B}
 * Sorcery
 * Destroy target land.
 */
val RainofTears = card("Rain of Tears") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetPermanent(filter = TargetFilter.Land))
        effect = MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Eric Peterson"
        flavorText = "When emotions rain down, what rivers will be born?"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/803ba4ef-24ed-4f45-aed8-f9442322e31e.jpg"
    }
}
