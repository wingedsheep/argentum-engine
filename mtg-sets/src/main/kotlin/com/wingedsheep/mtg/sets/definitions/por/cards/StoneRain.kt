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
 * Stone Rain
 * {2}{R}
 * Sorcery
 * Destroy target land.
 */
val StoneRain = card("Stone Rain") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetPermanent(filter = TargetFilter.Land))
        effect = MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "151"
        artist = "John Matson"
        flavorText = "I cast a thousand tiny suns—\nBeware my many dawns."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57f84a13-d7dc-491b-a77c-1b99b6797d7e.jpg"
    }
}
