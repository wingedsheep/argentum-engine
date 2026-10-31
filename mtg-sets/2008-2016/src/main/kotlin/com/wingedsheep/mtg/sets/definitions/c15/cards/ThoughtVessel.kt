package com.wingedsheep.mtg.sets.definitions.c15.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Thought Vessel
 * {2}
 * Artifact
 *
 * You have no maximum hand size.
 * {T}: Add {C}.
 */
val ThoughtVessel = card("Thought Vessel") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "You have no maximum hand size.\n{T}: Add {C}."

    staticAbility {
        ability = NoMaximumHandSize()
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "rk post"
        flavorText = "Infinite possibilities contained in a finite space."
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0cd769e-1aaf-458c-849f-3b6ebc7fd8c5.jpg?1562710607"
    }
}
