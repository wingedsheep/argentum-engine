package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddManaEffect

/**
 * Sandsteppe Citadel
 * Land
 * This land enters tapped.
 * {T}: Add {W}, {B}, or {G}.
 */
val SandsteppeCitadel = card("Sandsteppe Citadel") {
    typeLine = "Land"
    oracleText = "This land enters tapped.\n{T}: Add {W}, {B}, or {G}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.BLACK)
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
        rarity = Rarity.UNCOMMON
        collectorNumber = "241"
        artist = "Sam Burley"
        flavorText = "That which endures, survives."
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2dd40d90-c939-458a-9a98-27d10da6ff2f.jpg?1562784373"
    }
}
