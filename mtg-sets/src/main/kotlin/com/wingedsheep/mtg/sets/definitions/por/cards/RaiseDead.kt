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
 * Raise Dead
 * {B}
 * Sorcery
 * Return target creature card from your graveyard to your hand.
 */
val RaiseDead = card("Raise Dead") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetObject(filter = TargetFilter.CreatureInYourGraveyard))
        effect = MoveToZoneEffect(t, Zone.HAND)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Charles Gillespie"
        flavorText = "The earth cannot hold that which magic commands."
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0584553-a25e-4030-ab39-53550cba3f0b.jpg"
    }
}
