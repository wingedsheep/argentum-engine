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
 * Avengers Hangar
 * Land
 * This land enters tapped.
 * When this land enters, you gain 1 life.
 * {T}: Add {W} or {U}.
 *
 * Implementation note: the standard "gain land" dual cycle — an [EntersTapped] replacement
 * effect, an enters-the-battlefield trigger gaining 1 life, and two separate single-colour
 * mana abilities (a mana ability may only produce one of the two colours per activation).
 */
val AvengersHangar = card("Avengers Hangar") {
    manaCost = ""
    colorIdentity = "WU"
    typeLine = "Land"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {W} or {U}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "259"
        artist = "Pablo Mendoza"
        flavorText = "\"I made some modifications I think you'll like. You were a test pilot, weren't you, Carol? Want to take the new Quinjet for a spin?\"\n—Tony Stark"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5d03ebb-1ce9-4a6b-b13e-a72f0b075ae8.jpg?1783902886"
    }
}
