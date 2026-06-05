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
 * Wrath of God
 * {2}{W}{W}
 * Sorcery
 * Destroy all creatures. They can't be regenerated.
 */
val WrathofGod = card("Wrath of God") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    spell {
        effect = ForEachInGroupEffect(GroupFilter(GameObjectFilter.Creature), MoveToZoneEffect(EffectTarget.Self, Zone.GRAVEYARD, byDestruction = true), noRegenerate = true)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Mike Raabe"
        flavorText = "\"As flies to wanton boys, are we to the gods. They kill us for their sport.\"\n—William Shakespeare, King Lear"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d75d8204-6f9d-4a7a-bb8b-d51ac65a30fa.jpg"
    }
}
