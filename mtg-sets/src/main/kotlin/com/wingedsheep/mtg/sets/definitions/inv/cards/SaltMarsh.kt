package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Salt Marsh
 * Land
 * This land enters tapped.
 * {T}: Add {U} or {B}.
 */
val SaltMarsh = card("Salt Marsh") {
    typeLine = "Land"
    colorIdentity = "UB"
    oracleText = "This land enters tapped.\n{T}: Add {U} or {B}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "326"
        artist = "Jerry Tiritilli"
        flavorText = "Only death breeds in stagnant water.\n—Urborg saying"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed64934b-0e64-4b2f-97aa-c3fb7e6ce0b0.jpg?1562942685"
    }
}
