package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Molten Psyche
 * {1}{R}{R}
 * Sorcery
 * Each player shuffles the cards from their hand into their library, then draws that many cards.
 * Metalcraft — If you control three or more artifacts, Molten Psyche deals damage to each opponent
 * equal to the number of cards that player has drawn this turn.
 *
 * The first sentence is Winds of Change ([Patterns.Hand.wheelEffect]). Metalcraft is checked as the
 * spell resolves, after the draws, so the per-opponent count ([DynamicAmounts.cardsDrawnThisTurn]
 * evaluated as each opponent inside [Effects.ForEachPlayer]) includes the cards just drawn as well
 * as every other card that player drew this turn.
 */
val MoltenPsyche = card("Molten Psyche") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Each player shuffles the cards from their hand into their library, then draws that many cards.\n" +
        "Metalcraft — If you control three or more artifacts, Molten Psyche deals damage to each opponent equal to the number of cards that player has drawn this turn."

    spell {
        effect = Patterns.Hand.wheelEffect(Player.Each) then Effects.If(
            Conditions.YouControlAtLeast(3, GameObjectFilter.Artifact),
            Effects.ForEachPlayer(
                Player.EachOpponent,
                Effects.DealDamage(DynamicAmounts.cardsDrawnThisTurn(Player.You), EffectTarget.Controller)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Ryan Yee"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57e2382d-1f27-40d1-b809-c188c19ebc72.jpg?1783941723"
        ruling("2011-01-01", "If a player doesn't have any cards in their hand, the player will still shuffle their library.")
        ruling("2011-01-01", "The metalcraft effect counts all cards each opponent drew for any reason during that turn, not just the cards those opponents drew due to Molten Psyche's first effect.")
    }
}
