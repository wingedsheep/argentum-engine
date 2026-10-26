package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Nyx Lotus
 * {4}
 * Legendary Artifact
 * Nyx Lotus enters tapped.
 * {T}: Choose a color. Add an amount of mana of that color equal to your devotion to that color.
 */
val NyxLotus = card("Nyx Lotus") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "Nyx Lotus enters tapped.\n{T}: Choose a color. Add an amount of mana of that color equal to your devotion to that color. " +
        "(Your devotion to a color is the number of mana symbols of that color in the mana costs of permanents you control.)"
    replacementEffect(EntersTapped())
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(DynamicAmounts.devotionToChosenColor())
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Choose a color. Add an amount of mana of that color equal to your devotion to that color."
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "235"
        artist = "Raoul Vitale"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5bd6353f-d119-40e6-895c-030a11a7a2fe.jpg?1783931514"
    }
}
