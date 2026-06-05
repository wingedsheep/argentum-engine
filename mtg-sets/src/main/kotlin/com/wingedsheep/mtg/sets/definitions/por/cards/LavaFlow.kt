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
 * Lava Flow
 * {3}{R}{R}
 * Sorcery
 * Destroy target creature or land.
 */
val LavaFlow = card("Lava Flow") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetPermanent(filter = TargetFilter.CreatureOrLandPermanent))
        effect = MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Mike Dringenberg"
        flavorText = "People ran as never before at the thought of being buried and cremated at the same time."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89e825e4-98be-49f0-bc5e-c8988118dcef.jpg"
    }
}
