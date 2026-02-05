package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.DynamicAmount
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
        effect = DealDamageEffect(DynamicAmount.XValue, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Michael Sutfin"
        flavorText = "Fire answers to no one."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f175c959-3b5d-46a3-9194-fad2359bbff9.jpg"
    }
}
