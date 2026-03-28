package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.conditions.AnyCondition

/**
 * Bonecache Overseer
 * {B}
 * Creature — Squirrel Warlock
 * 1/1
 *
 * {T}, Pay 1 life: Draw a card. Activate only if three or more cards left your
 * graveyard this turn or if you've sacrificed a Food this turn.
 */
val BonecacheOverseer = card("Bonecache Overseer") {
    manaCost = "{B}"
    typeLine = "Creature — Squirrel Warlock"
    power = 1
    toughness = 1
    oracleText = "{T}, Pay 1 life: Draw a card. Activate only if three or more cards " +
        "left your graveyard this turn or if you've sacrificed a Food this turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1))
        effect = Effects.DrawCards(1)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                AnyCondition(listOf(
                    Conditions.CardsLeftGraveyardThisTurn(3),
                    Conditions.SacrificedFoodThisTurn
                ))
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "85"
        artist = "Mila Pesic"
        flavorText = "\"Bring me my harvest.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/2/82defb87-237f-4b77-9673-5bf00607148f.jpg?1721426349"
        ruling("2024-07-26", "Each time a card leaves your graveyard in a turn, it counts as one of three cards the activated ability is looking for. For example, if you return two cards from your graveyard to your hand, discard one of them, then exile that card from your graveyard, you can activate Bonecache Overseer's ability that turn.")
    }
}
