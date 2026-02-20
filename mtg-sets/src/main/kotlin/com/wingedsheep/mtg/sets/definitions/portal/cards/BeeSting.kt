package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.AnyTarget

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
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23bcf64a-ae3d-4abb-acc7-81bba237f37b.jpg"
    }
}
