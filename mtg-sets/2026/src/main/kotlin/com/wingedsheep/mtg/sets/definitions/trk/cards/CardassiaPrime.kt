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
 * Cardassia Prime
 * Land — Planet
 *
 * This land enters tapped.
 * When this land enters, you gain 1 life.
 * {T}: Add {W} or {B}.
 */
val CardassiaPrime = card("Cardassia Prime") {
    typeLine = "Land — Planet"
    colorIdentity = "BW"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {W} or {B}."
    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.WHITE)
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
        collectorNumber = "280"
        artist = "Alex Negrea"
        flavorText = "\"Gul's log: I return to find the entire city of Lakat wiped away, our Dominion allies licking wounds, and the damned Federation offering support. I have swallowed many a rancid meal in this war, but nothing so bitter as my own pride.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d634424f-9148-46a5-95e3-4965a9226bb4.jpg?1792632933"
    }
}

