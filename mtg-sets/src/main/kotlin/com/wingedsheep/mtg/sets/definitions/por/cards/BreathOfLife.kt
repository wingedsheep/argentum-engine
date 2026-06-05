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
 * Breath of Life
 * {3}{W}
 * Sorcery
 * Return target creature card from your graveyard to the battlefield.
 */
val BreathofLife = card("Breath of Life") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetObject(filter = TargetFilter.CreatureInYourGraveyard))
        effect = MoveToZoneEffect(t, Zone.BATTLEFIELD)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "DiTerlizzi"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcea5e09-6385-41df-970b-ac26c9b46127.jpg"
    }
}
