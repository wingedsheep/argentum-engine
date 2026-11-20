package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mutant's Prey
 * {G}
 * Instant
 * Target creature you control with a +1/+1 counter on it fights target creature an opponent
 * controls. (Each deals damage equal to its power to the other.)
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val MutantsPrey = card("Mutant's Prey") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature you control with a +1/+1 counter on it fights target creature an opponent controls. (Each deals damage equal to its power to the other.)"

    spell {
        val yourCreature = target(
            TargetFilter(GameObjectFilter.Creature.youControl().withCounter(CounterType.PLUS_ONE_PLUS_ONE))
        )
        val theirCreature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Fight(yourCreature, theirCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Ryan Barger"
        flavorText = "Every street corner is a hunting ground, every citizen potential prey."
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9e32d47-2796-4eac-b373-a93506d8d6b7.jpg"
    }
}
