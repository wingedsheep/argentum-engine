package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Maw of the Obzedat
 * {3}{W}{B}
 * Creature — Thrull
 * 3/3
 * Sacrifice a creature: Creatures you control get +1/+1 until end of turn.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val MawOfTheObzedat = card("Maw of the Obzedat") {
    manaCost = "{3}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Thrull"
    power = 3
    toughness = 3
    oracleText = "Sacrifice a creature: Creatures you control get +1/+1 until end of turn."

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Creature)
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.ModifyStats(1, 1, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Randy Gallegos"
        flavorText = "The devout sacrifice themselves to it hoping for escape from their debts to the guild, but many end up owing in the afterlife."
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd1131c6-04da-4c4d-ab61-874ac5be7087.jpg"
    }
}
