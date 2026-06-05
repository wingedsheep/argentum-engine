// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount


/**
 * Cruel Bargain
 * {B}{B}{B}
 * Sorcery
 * Draw four cards. You lose half your life, rounded up.
 */
val CruelBargain = card("Cruel Bargain") {
    manaCost = "{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    spell {
        effect = CompositeEffect(
        listOf(
            DrawCardsEffect(4),
            LoseLifeEffect(DynamicAmount.Divide(DynamicAmount.LifeTotal(Player.You), DynamicAmount.Fixed(2), roundUp = true), EffectTarget.Controller)
        )
    )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "86"
        artist = "Adrian Smith"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96837a9e-dd68-4ce8-b760-0e1c22837164.jpg"
    }
}
