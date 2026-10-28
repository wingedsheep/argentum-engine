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
 * Qo'noS
 * Land — Planet
 *
 * This land enters tapped.
 * When this land enters, you gain 1 life.
 * {T}: Add {B} or {R}.
 */
val QonoS = card("Qo'noS") {
    typeLine = "Land — Planet"
    colorIdentity = "BR"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {B} or {R}."
    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "291"
        artist = "Rockey Chen"
        flavorText = "\"General's log: We return triumphant! Let the streets of Qo'noS quake, for her warriors return with a hunger for new battle!\""
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1558d93-8a8a-45e0-bcdc-e6cd42f716dc.jpg?1792632963"
    }
}

