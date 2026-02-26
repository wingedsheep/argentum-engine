package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Flamewave Invoker
 * {2}{R}
 * Creature — Goblin Mutant
 * 2/2
 * {7}{R}: Flamewave Invoker deals 5 damage to target player or planeswalker.
 */
val FlamewaveInvoker = card("Flamewave Invoker") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Goblin Mutant"
    power = 2
    toughness = 2
    oracleText = "{7}{R}: Flamewave Invoker deals 5 damage to target player or planeswalker."

    activatedAbility {
        cost = AbilityCost.Mana(ManaCost.parse("{7}{R}"))
        val t = target("target player or planeswalker", TargetPlayer())
        effect = DealDamageEffect(5, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "92"
        artist = "Dave Dorman"
        flavorText = "The Mirari burns in his heart."
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13a68534-2d9a-47e9-9d2a-cb6df4362aa9.jpg?1562898919"
    }
}
