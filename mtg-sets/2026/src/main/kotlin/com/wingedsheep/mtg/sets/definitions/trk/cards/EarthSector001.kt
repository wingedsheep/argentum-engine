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
 * Earth, Sector 001
 * Land — Planet
 *
 * This land enters tapped.
 * When this land enters, you gain 1 life.
 * {T}: Add {G} or {U}.
 */
val EarthSector001 = card("Earth, Sector 001") {
    typeLine = "Land — Planet"
    colorIdentity = "GU"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {G} or {U}."
    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "281"
        artist = "Colin Searle"
        flavorText = "\"Subcommander's log: We approach the human homeworld. I am curious to witness it myself. Perhaps it will yield insight into their illogical yet accomplished culture.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/2/5278d354-97c4-4c21-a902-2f9252b151eb.jpg?1792632943"
    }
}

