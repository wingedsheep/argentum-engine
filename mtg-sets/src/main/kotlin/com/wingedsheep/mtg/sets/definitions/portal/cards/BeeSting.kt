package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.AnyTarget

/**
 * Bee Sting
 * {3}{G}
 * Sorcery
 * Bee Sting deals 2 damage to any target.
 */
val BeeSting = card("Bee Sting") {
    manaCost = "{3}{G}"
    typeLine = "Sorcery"

    spell {
        target = AnyTarget()
        effect = DealDamageEffect(2, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Thomas M. Baxa"
        flavorText = "Nature's smallest warriors pack a powerful punch."
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6de8f9a0-b1c2-d3e4-f5a6-b7c8d9e0f1a2.jpg"
    }
}
