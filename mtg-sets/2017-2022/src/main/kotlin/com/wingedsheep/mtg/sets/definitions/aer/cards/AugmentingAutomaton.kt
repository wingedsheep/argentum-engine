package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Augmenting Automaton
 * {1}
 * Artifact Creature — Construct
 * 1/1
 * {1}{B}: This creature gets +1/+1 until end of turn.
 */
val AugmentingAutomaton = card("Augmenting Automaton") {
    manaCost = "{1}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Construct"
    power = 1
    toughness = 1
    oracleText = "{1}{B}: This creature gets +1/+1 until end of turn."

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "143"
        artist = "Jason Rainville"
        flavorText = "When you have taught an automaton how to build others of its kind, it is a small step to make it build itself."
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d017798-8278-4f9c-a691-912935c10c20.jpg?1783936731"
    }
}
