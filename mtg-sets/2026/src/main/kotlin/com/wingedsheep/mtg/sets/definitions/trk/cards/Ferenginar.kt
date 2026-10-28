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
 * Ferenginar
 * Land — Planet
 *
 * This land enters tapped.
 * When this land enters, you gain 1 life.
 * {T}: Add {B} or {G}.
 */
val Ferenginar = card("Ferenginar") {
    typeLine = "Land — Planet"
    colorIdentity = "BG"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {B} or {G}."
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
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "283"
        artist = "Colin Searle"
        flavorText = "\"Operation chief's log: Heading back to the Ferengi homeworld to pick up more salvage. Raining again. The tubeworms aren't bad, but I just want to bite into something crisp again.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17fab75f-7ff9-4c50-a6f7-03a85cd275bf.jpg?1792632949"
    }
}

