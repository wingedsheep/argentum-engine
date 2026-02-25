package com.wingedsheep.mtg.sets.definitions.khans.cards

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
 * Rugged Highlands
 * Land
 * This land enters tapped.
 * When this land enters, you gain 1 life.
 * {T}: Add {R} or {G}.
 */
val RuggedHighlands = card("Rugged Highlands") {
    typeLine = "Land"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {R} or {G}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "240"
        artist = "Eytan Zana"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/501ce6cb-0324-4cca-bc79-903cefe1ac1f.jpg?1562786462"
    }
}
