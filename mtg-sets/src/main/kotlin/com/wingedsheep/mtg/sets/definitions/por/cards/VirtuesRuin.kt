// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget


/**
 * Virtue's Ruin
 * {2}{B}
 * Sorcery
 * Destroy all white creatures.
 */
val VirtuesRuin = card("Virtue's Ruin") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    spell {
        effect = ForEachInGroupEffect(GroupFilter(GameObjectFilter.Creature.withColor(Color.WHITE)), MoveToZoneEffect(EffectTarget.Self, Zone.GRAVEYARD, byDestruction = true), noRegenerate = false)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Mike Dringenberg"
        flavorText = "All must fall, and those who stand highest fall hardest."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/7854928a-d467-4616-b96b-de7e5fe7303e.jpg"
    }
}
