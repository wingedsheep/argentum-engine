package com.wingedsheep.mtg.sets.definitions.ddn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Nomad Outpost
 * Land
 * This land enters tapped.
 * {T}: Add {R}, {W}, or {B}.
 */
val NomadOutpost = card("Nomad Outpost") {
    typeLine = "Land"
    colorIdentity = "WBR"
    oracleText = "This land enters tapped.\n{T}: Add {R}, {W}, or {B}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
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
        rarity = Rarity.UNCOMMON
        collectorNumber = "34"
        artist = "Noah Bradley"
        flavorText = "\"Only the weak imprison themselves behind walls. We live free under the wind, and our freedom makes us strong.\"\n—Zurgo, khan of the Mardu"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34f492d6-8ca9-4360-8608-9183bdb0eed8.jpg?1783939118"
    }
}
