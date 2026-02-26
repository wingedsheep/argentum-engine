package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stonewood Invoker
 * {1}{G}
 * Creature — Elf Mutant
 * 2/2
 * {7}{G}: Stonewood Invoker gets +5/+5 until end of turn.
 */
val StonewoodInvoker = card("Stonewood Invoker") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Elf Mutant"
    power = 2
    toughness = 2
    oracleText = "{7}{G}: This creature gets +5/+5 until end of turn."

    activatedAbility {
        cost = AbilityCost.Mana(ManaCost.parse("{7}{G}"))
        effect = Effects.ModifyStats(5, 5, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Eric Peterson"
        flavorText = "The Mirari pulses in his veins."
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94d0235d-7176-44a2-8e95-eb231f4af441.jpg?1562925012"
    }
}
