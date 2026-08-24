package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thrashing Wumpus
 * {3}{B}{B}
 * Creature — Beast
 * 3 / 3
 * {B}: This creature deals 1 damage to each creature and each player.
 *
 * "Each creature and each player" is two iterations, not one: a group pass over the creatures
 * (`EffectTarget.Self` = the current iteration entity) and a player pass where each iteration
 * rebinds the controller (`EffectTarget.Controller`). Same idiom as Inferno.
 */
val ThrashingWumpus = card("Thrashing Wumpus") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Beast"
    oracleText = "{B}: This creature deals 1 damage to each creature and each player."
    power = 3
    toughness = 3

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = Effects.Composite(
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature),
                Effects.DealDamage(1, EffectTarget.Self)
            ),
            Effects.ForEachPlayer(
                Player.Each,
                listOf(Effects.DealDamage(1, EffectTarget.Controller))
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "166"
        artist = "Jeff Miracola"
        flavorText = "Young wumpuses are malevolent and vicious—but they grow out of it."
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86bc07c6-2ba7-41f8-90ab-f9bbac86dd08.jpg"
    }
}
