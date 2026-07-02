package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Chart a Course — Ixalan #48 (canonical printing)
 * {1}{U} · Sorcery
 *
 * Draw two cards. Then discard a card unless you attacked this turn.
 *
 * Draw is unconditional; the discard is gated on the negation of [Conditions.YouAttackedThisTurn]
 * so it only happens when you did not declare an attacker this turn. The condition is evaluated at
 * resolution (CR 608.2), after the two cards are drawn, so the discard sees your post-draw hand.
 */
val ChartACourse = card("Chart a Course") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Draw two cards. Then discard a card unless you attacked this turn."

    spell {
        effect = Effects.DrawCards(2).then(
            ConditionalEffect(
                condition = Conditions.Not(Conditions.YouAttackedThisTurn),
                effect = Effects.Discard(1),
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "James Ryman"
        flavorText = "While other pirates prowl for treasure, Captain Parrish plunders secrets."
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98291778-2ec2-47e2-ac99-5f8cfbb3cf24.jpg?1782710474"
    }
}
