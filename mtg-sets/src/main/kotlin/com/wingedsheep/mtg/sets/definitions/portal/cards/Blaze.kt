package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.AnyTarget

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
        val t = target("target", AnyTarget())
        effect = DealDamageEffect(DynamicAmount.XValue, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Michael Sutfin"
        flavorText = "Fire answers to no one."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f175c959-3b5d-46a3-9194-fad2359bbff9.jpg"
    }
}
