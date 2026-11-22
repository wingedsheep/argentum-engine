package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Frilled Oculus
 * {1}{U}
 * Creature — Homunculus
 * 1/3
 * {1}{G}: This creature gets +2/+2 until end of turn. Activate only once each turn.
 */
val FrilledOculus = card("Frilled Oculus") {
    manaCost = "{1}{U}"
    colorIdentity = "UG"
    typeLine = "Creature — Homunculus"
    oracleText = "{1}{G}: This creature gets +2/+2 until end of turn. Activate only once each turn."
    power = 1
    toughness = 3

    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.OncePerTurn)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "35"
        artist = "Marco Nelor"
        flavorText = "\"Understand the Simic? That's about as easy as winning a staring contest with a homunculus.\"\n—Dars Gostok, Firefist captain"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9f3a08f-403e-4d6c-87c7-add8170bde8b.jpg"
    }
}
