package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.AnyTarget

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
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e4817bd-68e8-4a85-983a-ee6dda2bbf33.jpg"
    }
}
