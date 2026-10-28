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
 * Bajor
 * Land — Planet
 *
 * This land enters tapped.
 * When this land enters, you gain 1 life.
 * {T}: Add {R} or {W}.
 */
val Bajor = card("Bajor") {
    typeLine = "Land — Planet"
    colorIdentity = "RW"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {R} or {W}."
    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(1)
    }

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
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "277"
        artist = "John Di Giovanni"
        flavorText = "\"Commander's log: I'm taking Jake to tour Rakantha Province. We can serve these people only so well without meeting them where they live.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff5254a6-4961-4604-b65b-2eb0ce9b1c82.jpg?1792632930"
    }
}

