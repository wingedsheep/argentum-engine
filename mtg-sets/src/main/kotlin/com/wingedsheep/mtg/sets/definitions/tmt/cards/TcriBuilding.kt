package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddManaEffect

/**
 * TCRI Building
 * Land
 *
 * This land enters tapped.
 * When this land enters, you gain 1 life.
 * {T}: Add {U} or {R}.
 */
val TcriBuilding = card("TCRI Building") {
    typeLine = "Land"
    colorIdentity = "UR"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {U} or {R}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "189"
        artist = "Aenami"
        flavorText = "The light from the Translocation Matrix could be seen throughout the city. The utroms knew they only had a brief window to escape after its accidental activation."
        imageUri = "https://cards.scryfall.io/normal/front/8/8/8817a1d6-ef39-4e7c-8277-74aea012803b.jpg?1771587112"
    }
}
