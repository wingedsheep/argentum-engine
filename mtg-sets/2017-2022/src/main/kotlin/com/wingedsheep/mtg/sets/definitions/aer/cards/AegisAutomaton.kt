package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Aegis Automaton
 * {2}
 * Artifact Creature — Construct
 * 0/3
 * {4}{W}: Return another target creature you control to its owner's hand.
 */
val AegisAutomaton = card("Aegis Automaton") {
    manaCost = "{2}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Construct"
    power = 0
    toughness = 3
    oracleText = "{4}{W}: Return another target creature you control to its owner's hand."

    activatedAbility {
        cost = Costs.Mana("{4}{W}")
        val creature = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.ReturnToHand(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "141"
        artist = "Kieran Yanner"
        flavorText = "\"The streets of Ghirapur have become dangerous. It's good to have a dependable companion.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5f9fa2d-4bf4-4fcb-9b76-fd4a9ff5a58c.jpg?1783936731"
    }
}
