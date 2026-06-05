// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature


/**
 * Hand of Death
 * {2}{B}
 * Sorcery
 * Destroy target nonblack creature.
 */
val HandofDeath = card("Hand of Death") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature.notColor(Color.BLACK)))
        effect = MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "John Coulthart"
        flavorText = "Death claims all but the darkest souls."
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27f136b8-52be-49b9-919b-2b9785254350.jpg"
    }
}
