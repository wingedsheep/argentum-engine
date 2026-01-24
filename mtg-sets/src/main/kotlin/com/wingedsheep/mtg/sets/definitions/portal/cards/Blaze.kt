package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealXDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.AnyTarget

/**
 * Blaze
 * {X}{R}
 * Sorcery
 * Blaze deals X damage to any target.
 */
val Blaze = card("Blaze") {
    manaCost = "{X}{R}"
    typeLine = "Sorcery"

    spell {
        target = AnyTarget()
        effect = DealXDamageEffect(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Michael Sutfin"
        flavorText = "Fire answers to no one."
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f0a1b2c-9d0e-1f2a-3b4c-5d6e7f8a9b0c.jpg"
    }
}
