package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Frilled Sandwalla (HOU #116)
 * {G}  Creature — Lizard  1/1
 *
 * {1}{G}: This creature gets +2/+2 until end of turn. Activate only once each turn.
 */
val FrilledSandwalla = card("Frilled Sandwalla") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Lizard"
    oracleText = "{1}{G}: This creature gets +2/+2 until end of turn. Activate only once each turn."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "116"
        artist = "Zezhou Chen"
        flavorText = "It basks atop the rubble of Rhonas's monument, which still pulses with latent power."
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07b1be52-5e54-4840-bb53-0b2512785e0b.jpg"
    }
}
