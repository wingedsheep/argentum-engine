package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.TauntEffect
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Taunt
 * {U}
 * Sorcery
 * During target player's next turn, creatures that player controls attack you if able.
 */
val Taunt = card("Taunt") {
    manaCost = "{U}"
    typeLine = "Sorcery"

    spell {
        target = TargetPlayer()
        effect = TauntEffect(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "71"
        artist = "Jeff Miracola"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4d87322-aba4-4187-9655-1da1f18615f8.jpg"
    }
}
