package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.AnyTarget

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
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18c3d4e5-f6a7-b8c9-d0e1-f2a3b4c5d6e7.jpg"
    }
}
