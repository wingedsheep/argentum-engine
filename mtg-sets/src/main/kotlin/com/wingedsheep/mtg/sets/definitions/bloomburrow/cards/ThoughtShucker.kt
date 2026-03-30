package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thought Shucker
 * {1}{U}
 * Creature — Rat Rogue
 * 1/3
 *
 * Threshold — {1}{U}: Put a +1/+1 counter on this creature and draw a card.
 * Activate only if there are seven or more cards in your graveyard and only once.
 */
val ThoughtShucker = card("Thought Shucker") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Rat Rogue"
    power = 1
    toughness = 3
    oracleText = "Threshold — {1}{U}: Put a +1/+1 counter on this creature and draw a card. Activate only if there are seven or more cards in your graveyard and only once."

    activatedAbility {
        cost = Costs.Mana("{1}{U}")
        effect = Effects.AddCounters("plus1plus1", 1, EffectTarget.Self)
            .then(Effects.DrawCards(1))
        restrictions = listOf(
            ActivationRestriction.All(
                ActivationRestriction.OnlyIfCondition(Conditions.CardsInGraveyardAtLeast(7)),
                ActivationRestriction.Once
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Dave Kendall"
        flavorText = "Many rats have found their minds while lost in the mud-slicked gyre of a snail shell."
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44b0d83b-cc41-4f82-892c-ef6d3293228a.jpg?1721426302"
    }
}
