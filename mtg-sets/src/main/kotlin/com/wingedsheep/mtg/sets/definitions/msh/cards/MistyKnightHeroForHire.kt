package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Misty Knight, Hero for Hire — Marvel Super Heroes #145 (uncommon)
 * {1}{R} · Legendary Creature — Human Detective Hero · 3/1
 *
 * {2}, {T}, Discard a card: Draw a card for each card you've discarded this turn.
 *
 * Implementation notes:
 * - A three-atom [Costs.Composite]: mana, the tap symbol, and [Costs.DiscardCard]. Costs are paid
 *   on activation, so the card discarded to pay for this ability is already counted when the draw
 *   resolves — the first activation of a turn always draws at least one card.
 * - "for each card you've discarded this turn" is [DynamicAmounts.cardsDiscardedThisTurn] (the
 *   per-player `CARDS_DISCARDED` turn tracker, defaulting to you), read at resolution — the Green
 *   Goblin, Revenant shape. It counts *every* discard you made this turn, from any source
 *   (cleanup-step discards, opponent-forced discards, other conniving permanents), not just the
 *   ones this ability paid for.
 * - Nothing about the ability is targeted, so it resolves even if Misty Knight has left the
 *   battlefield in response.
 */
val MistyKnightHeroForHire = card("Misty Knight, Hero for Hire") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Detective Hero"
    power = 3
    toughness = 1
    oracleText = "{2}, {T}, Discard a card: Draw a card for each card you've discarded this turn."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.DiscardCard,
        )
        effect = Effects.DrawCards(DynamicAmounts.cardsDiscardedThisTurn())
        description = "{2}, {T}, Discard a card: Draw a card for each card you've discarded this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "145"
        artist = "Eglė Mosakaitė"
        flavorText = "\"Hello, hero. This is Control. Are you for hire tonight?\""
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b963642-a103-44a0-9eb7-9c3fdde181b7.jpg?1783902927"
    }
}
