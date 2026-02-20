package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Volcanic Hammer
 * {1}{R}
 * Sorcery
 * Volcanic Hammer deals 3 damage to any target.
 */
val VolcanicHammer = card("Volcanic Hammer") {
    manaCost = "{1}{R}"
    typeLine = "Sorcery"

    spell {
        target = AnyTarget()
        effect = DealDamageEffect(3, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "154"
        artist = "Brian Snoddy"
        flavorText = "Forged in the heart of a volcano."
        imageUri = "https://cards.scryfall.io/normal/front/9/5/9563d7c1-4ed1-4919-b0b8-ea1ec9d4bbf6.jpg"
    }
}
