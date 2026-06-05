// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.targets.TargetOpponentOrPlaneswalker


/**
 * Vampiric Touch
 * {2}{B}
 * Sorcery
 * Vampiric Touch deals 2 damage to target opponent or planeswalker and you gain 2 life.
 */
val VampiricTouch = card("Vampiric Touch") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetOpponentOrPlaneswalker())
        effect = CompositeEffect(
        listOf(
            DealDamageEffect(2, t),
            GainLifeEffect(2)
        )
    )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "115"
        artist = "Zina Saunders"
        flavorText = "A touch, not comforting, but of death."
        imageUri = "https://cards.scryfall.io/normal/front/2/3/231f7598-8c47-4828-8240-e2a545a7ac5b.jpg"
    }
}
