package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Romulus
 * Land — Planet
 *
 * This land enters tapped.
 * When this land enters, you gain 1 life.
 * {T}: Add {U} or {B}.
 */
val Romulus = card("Romulus") {
    typeLine = "Land — Planet"
    colorIdentity = "BU"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {U} or {B}."
    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "294"
        artist = "Carlos Dattoli"
        flavorText = "\"Centurion's log: With the commander's detainment at the hands of the Tal Shiar, it seems likely I will see a promotion upon our return. A shame she did not conceal her treachery better.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1bce111b-b07c-4589-9b6d-f0b5f320808c.jpg?1792632971"
    }
}

