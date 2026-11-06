package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Welder Automaton
 * {2}
 * Artifact Creature — Construct
 * 2/1
 * {3}{R}: This creature deals 1 damage to each opponent.
 */
val WelderAutomaton = card("Welder Automaton") {
    manaCost = "{2}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Construct"
    power = 2
    toughness = 1
    oracleText = "{3}{R}: This creature deals 1 damage to each opponent."

    activatedAbility {
        cost = Costs.Mana("{3}{R}")
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "183"
        artist = "Victor Adame Minguez"
        flavorText = "Most of the constructs the renegades used against the Consulate weren't originally " +
            "intended for combat. But some didn't require much modification."
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88b5bae4-be97-4c10-b222-e1e317a8ffbc.jpg"
    }
}
