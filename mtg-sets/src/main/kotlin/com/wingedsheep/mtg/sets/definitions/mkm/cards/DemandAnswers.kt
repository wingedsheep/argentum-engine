package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Demand Answers — Murders at Karlov Manor #122
 * {1}{R} · Instant
 *
 * As an additional cost to cast this spell, sacrifice an artifact or discard a card.
 * Draw two cards.
 *
 * Cost-vs-cost additional cost (`Costs.additional.Choice`, the Souls of the Lost shape): each
 * branch surfaces as its own cast action, so the player picks the sub-cost by picking which
 * action to play, and a branch with nothing to pay it with simply isn't offered. Because it's
 * an *additional cost*, it is paid on casting — countering the spell doesn't give the artifact
 * or the discarded card back. Sacrificing a Clue to it is a real sacrifice, so "when you
 * sacrifice a Clue" payoffs (Curious Cadaver) see it.
 */
val DemandAnswers = card("Demand Answers") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, sacrifice an artifact or discard a card.\n" +
        "Draw two cards."

    additionalCost(
        Costs.additional.Choice(
            Costs.additional.SacrificePermanent(GameObjectFilter.Artifact),
            Costs.additional.DiscardCards()
        )
    )

    spell {
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "122"
        artist = "Justyna Dura"
        flavorText = "After the attempt on Aurelia's life, she gave the Agency an ultimatum: solve " +
            "the case in twenty-four hours, or the Boros Legion would declare war on the Cult of Rakdos."
        imageUri = "https://cards.scryfall.io/normal/front/e/c/eca092fc-7c67-4a73-989e-5297bbaaea76.jpg?1783912883"
    }
}
