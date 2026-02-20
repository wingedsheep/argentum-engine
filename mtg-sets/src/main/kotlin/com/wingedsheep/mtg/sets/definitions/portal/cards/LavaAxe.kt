package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Lava Axe
 * {4}{R}
 * Sorcery
 * Lava Axe deals 5 damage to target player or planeswalker.
 */
val LavaAxe = card("Lava Axe") {
    manaCost = "{4}{R}"
    typeLine = "Sorcery"

    spell {
        target = TargetPlayer()
        effect = DealDamageEffect(5, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "137"
        artist = "Brian Snoddy"
        flavorText = "The axe comes down, and the enemy goes up—in flames."
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2bebbad-76aa-4388-891a-583e8af9509d.jpg"
    }
}
