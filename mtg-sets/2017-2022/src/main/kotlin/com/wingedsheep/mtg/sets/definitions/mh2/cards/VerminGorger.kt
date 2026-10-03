package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vermin Gorger
 * {1}{B}
 * Creature — Vampire
 * 2/2
 * {T}, Sacrifice another creature: Each opponent loses 2 life and you gain 2 life.
 *
 * A tap-plus-sacrifice-another cost (Orc General's shape) feeding Kheru Bloodsucker's
 * drain-each-opponent-then-gain sequence.
 */
val VerminGorger = card("Vermin Gorger") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 2
    toughness = 2
    oracleText = "{T}, Sacrifice another creature: Each opponent loses 2 life and you gain 2 life."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeAnother(GameObjectFilter.Creature))
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Tobias Kwan"
        flavorText = "\"Once, vampires ruled this world, commanding the fear and respect that befit our noble blood. " +
            "Have we truly sunk so low?\"\n—Sorin Markov"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3166b10-5bc3-4db6-bb5b-81045d98e446.jpg?1783926853"
    }
}
