package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.TauntEffect
import com.wingedsheep.sdk.targeting.TargetPlayer

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
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d7ec7c0-cd22-4f92-8e41-16be49ab1adb.jpg"
    }
}
