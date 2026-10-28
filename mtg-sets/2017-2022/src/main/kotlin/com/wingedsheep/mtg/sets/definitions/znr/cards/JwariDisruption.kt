package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Jwari Disruption {1}{U} // Jwari Ruins — Zendikar Rising #64
 * Instant // Land · Modal double-faced card
 *
 * Front — Jwari Disruption: "Counter target spell unless its controller pays {1}."
 * Back — Jwari Ruins: "This land enters tapped. {T}: Add {U}."
 */
private val JwariDisruptionFront = card("Jwari Disruption") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {1}."

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessPays("{1}")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Cliff Childs"
        flavorText = "\"You worry too much.\"\n—Nys Dal, Sea Gate trapfinder"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/301750a7-d1fd-435e-bfa8-9d2fb22ad627.jpg?1783929400"
    }
}

private val JwariRuinsBack = card("Jwari Ruins") {
    typeLine = "Land"
    colorIdentity = "U"
    oracleText = "This land enters tapped.\n{T}: Add {U}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Cliff Childs"
        flavorText = "\"The Ondu Skyclave was a center for rebellion against the capital in Makindi. " +
            "Of course they built in deadly traps.\"\n—Nys Dal, Sea Gate trapfinder"
        imageUri = "https://cards.scryfall.io/normal/back/3/0/301750a7-d1fd-435e-bfa8-9d2fb22ad627.jpg?1783929400"
    }
}

val JwariDisruption: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = JwariDisruptionFront,
    backFace = JwariRuinsBack,
)
