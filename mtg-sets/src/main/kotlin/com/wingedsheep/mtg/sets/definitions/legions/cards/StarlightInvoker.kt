package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Starlight Invoker
 * {1}{W}
 * Creature — Human Cleric Mutant
 * 1/3
 * {7}{W}: You gain 5 life.
 */
val StarlightInvoker = card("Starlight Invoker") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Cleric Mutant"
    power = 1
    toughness = 3
    oracleText = "{7}{W}: You gain 5 life."

    activatedAbility {
        cost = AbilityCost.Mana(ManaCost.parse("{7}{W}"))
        effect = GainLifeEffect(5, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "20"
        artist = "Glen Angus"
        flavorText = "The Mirari glows in her eyes."
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c66afc4-3d6d-4ce7-acfc-a4ad34aa3e99.jpg?1562910443"
    }
}
