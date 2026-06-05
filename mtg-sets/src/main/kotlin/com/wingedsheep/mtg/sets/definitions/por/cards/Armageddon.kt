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
 * Armageddon
 * {3}{W}
 * Sorcery
 * Destroy all lands.
 */
val Armageddon = card("Armageddon") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    spell {
        effect = ForEachInGroupEffect(GroupFilter(GameObjectFilter.Land), MoveToZoneEffect(EffectTarget.Self, Zone.GRAVEYARD, byDestruction = true), noRegenerate = false)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "5"
        artist = "John Avon"
        flavorText = "\"'O miserable of happy! Is this the end Of this new glorious world . . . ?'\"\n—John Milton, Paradise Lost"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/2073ca8b-2bca-4539-94d7-989da157e4b8.jpg"
    }
}
