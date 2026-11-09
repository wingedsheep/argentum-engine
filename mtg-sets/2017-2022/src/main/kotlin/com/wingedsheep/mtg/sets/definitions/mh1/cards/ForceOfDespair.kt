package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostZone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost

/**
 * Force of Despair — Modern Horizons #92 (canonical printing)
 * {1}{B}{B} · Instant
 *
 * If it's not your turn, you may exile a black card from your hand rather than pay this spell's
 * mana cost.
 * Destroy all creatures that entered this turn.
 *
 * The Force cycle's free [SelfAlternativeCost] (Force of Vigor's shape, exiling a black card
 * instead of a green one, gated by [Conditions.IsNotYourTurn]) over a sweeper whose filter is
 * `enteredThisTurn()` — a creature that entered under *any* player's control this turn, tracked
 * from the event rather than from summoning sickness.
 */
val ForceOfDespair = card("Force of Despair") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "If it's not your turn, you may exile a black card from your hand rather than " +
        "pay this spell's mana cost.\nDestroy all creatures that entered this turn."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        additionalCosts = listOf(
            Costs.additional.ExileCards(
                count = 1,
                filter = GameObjectFilter.Any.withColor(Color.BLACK),
                fromZone = CostZone.HAND
            )
        ),
        condition = Conditions.IsNotYourTurn
    )

    spell {
        effect = Effects.DestroyAll(GameObjectFilter.Creature.enteredThisTurn())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f497b0d-4448-4201-bd55-c147da1a216d.jpg?1783933128"
    }
}
