package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.AnyTarget

/**
 * Scorching Spear
 * {R}
 * Sorcery
 * Scorching Spear deals 1 damage to any target.
 */
val ScorchingSpear = card("Scorching Spear") {
    manaCost = "{R}"
    typeLine = "Sorcery"

    spell {
        target = AnyTarget()
        effect = DealDamageEffect(1, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "Dan Frazier"
        flavorText = "A single point of flame can pierce the mightiest armor."
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2c9d0e1-f2a3-b4c5-d6e7-f8a9b0c1d2e3.jpg"
    }
}
