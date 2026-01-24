package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GainLifeEffect
import com.wingedsheep.sdk.targeting.TargetPlayer

/**
 * Natural Spring
 * {3}{G}{G}
 * Sorcery
 * Target player gains 8 life.
 */
val NaturalSpring = card("Natural Spring") {
    manaCost = "{3}{G}{G}"
    typeLine = "Sorcery"

    spell {
        target = TargetPlayer()
        effect = GainLifeEffect(
            amount = 8,
            target = EffectTarget.ContextTarget(0)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "176"
        artist = "Janine Johnston"
        flavorText = "The spring's waters carry the healing essence of the forest."
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e73e4a1-6f33-4c8f-b5e6-c5c48feeac8b.jpg"
    }
}
