package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect

/**
 * Command Bridge
 * Land
 * This land enters tapped.
 * When this land enters, sacrifice it unless you tap an untapped permanent you control.
 * {T}: Add one mana of any color.
 */
val CommandBridge = card("Command Bridge") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "This land enters tapped.\n" +
        "When this land enters, sacrifice it unless you tap an untapped permanent you control.\n" +
        "{T}: Add one mana of any color."

    // This land enters tapped.
    replacementEffect(EntersTapped())

    // When this land enters, sacrifice it unless you tap an untapped permanent you control.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = PayOrSufferEffect(
            cost = Costs.pay.Tap(),
            suffer = SacrificeSelfEffect,
        )
        description = "When this land enters, sacrifice it unless you tap an untapped permanent you control."
    }

    // {T}: Add one mana of any color.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "252"
        artist = "Constantin Marin"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/247670d2-a7cd-4ed7-9c77-704c7962b815.jpg?1752947586"
    }
}
