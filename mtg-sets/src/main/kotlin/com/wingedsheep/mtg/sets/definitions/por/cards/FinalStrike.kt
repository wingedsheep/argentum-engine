// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.TargetOpponentOrPlaneswalker


/**
 * Final Strike
 * {2}{B}{B}
 * Sorcery
 * As an additional cost to cast this spell, sacrifice a creature.
 * Final Strike deals damage to target opponent or planeswalker equal to the sacrificed creature's power.
 */
val FinalStrike = card("Final Strike") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    additionalCost(AdditionalCost.SacrificePermanent(GameObjectFilter.Creature))
    spell {
        val t = target("target", TargetOpponentOrPlaneswalker())
        effect = DealDamageEffect(DynamicAmounts.sacrificedPower(), t)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "94"
        artist = "John Coulthart"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ecdfcb03-2f77-4f54-af62-3012cd3efd4f.jpg"
    }
}
