package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Los Diablos Missile Base
 * Land
 * This land enters tapped.
 * When this land enters, you gain 1 life.
 * {T}: Add {R} or {G}.
 *
 * Implementation note: the standard "gain land" dual cycle — an [EntersTapped] replacement
 * effect, an enters-the-battlefield trigger gaining 1 life, and two separate single-colour
 * mana abilities (a mana ability may only produce one of the two colours per activation).
 */
val LosDiablosMissileBase = card("Los Diablos Missile Base") {
    manaCost = ""
    colorIdentity = "RG"
    typeLine = "Land"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {R} or {G}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "270"
        artist = "Rockey Chen"
        flavorText = "\"My men have been stationed here for weeks because of your infernal delays, Banner! Are you going to test that gamma bomb or not?\"\n—General \"Thunderbolt\" Ross"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/2123fcb5-8181-47ab-9a2d-7ede5b5118e8.jpg?1783902883"
    }
}
