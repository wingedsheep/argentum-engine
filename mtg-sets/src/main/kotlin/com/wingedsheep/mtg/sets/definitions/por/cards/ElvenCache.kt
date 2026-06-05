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
import com.wingedsheep.sdk.scripting.targets.TargetObject


/**
 * Elven Cache
 * {2}{G}{G}
 * Sorcery
 * Return target card from your graveyard to your hand.
 */
val ElvenCache = card("Elven Cache") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetObject(filter = TargetFilter.CardInGraveyard))
        effect = MoveToZoneEffect(t, Zone.HAND)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Rebecca Guay"
        flavorText = "Elves know where to harvest the best of the forest, for they planted it themselves."
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68939020-eb6a-4d77-a850-4df96cf01918.jpg"
    }
}
