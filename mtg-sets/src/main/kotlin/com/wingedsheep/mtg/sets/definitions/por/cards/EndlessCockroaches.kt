// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget


/**
 * Endless Cockroaches
 * {1}{B}{B}
 * Creature — Insect
 * 1/1
 * When this creature dies, return it to its owner's hand.
 */
val EndlessCockroaches = card("Endless Cockroaches") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Insect"
    power = 1
    toughness = 1
    triggeredAbility {
        trigger = Triggers.Dies
        effect = MoveToZoneEffect(EffectTarget.Self, Zone.HAND)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d3d18b9-ad59-435b-934b-703e10287e32.jpg"
    }
}
