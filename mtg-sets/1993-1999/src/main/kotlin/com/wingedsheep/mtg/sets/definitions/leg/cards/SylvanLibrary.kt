package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sylvan Library
 * {1}{G}
 * Enchantment
 *
 * At the beginning of your draw step, you may draw two additional cards. If you do, choose two
 * cards in your hand drawn this turn. For each of those cards, pay 4 life or put the card on top
 * of your library.
 *
 * - "If you do" gates on the choice to draw, not on the draws succeeding: per the rulings, a
 *   replaced draw still leaves the rest of the ability to happen, over whatever was actually drawn.
 * - "Cards in your hand drawn this turn" is `drawnThisTurn()` (`StatePredicate.DrawnThisTurn`), so
 *   cards drawn in the upkeep or in response count too, and fewer than two such cards are all chosen.
 * - The per-card "pay 4 life or put it back" is one keep/put-back split, capped by
 *   `MaxAffordablePayment(lifePerSelected = 4)` so a kept card is always payable (CR 119.4), then one
 *   4-life payment per kept card. The put-back cards go on top in the order the player chooses.
 */
val SylvanLibrary = card("Sylvan Library") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your draw step, you may draw two additional cards. If you do, " +
        "choose two cards in your hand drawn this turn. For each of those cards, pay 4 life or put " +
        "the card on top of your library."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.DRAW)
        effect = Effects.May(
            Effects.Pipeline {
                run(Effects.DrawCards(2))
                val drawn = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Any.drawnThisTurn()))
                val chosen = chooseExactly(
                    2,
                    from = drawn,
                    prompt = "Choose two cards in your hand drawn this turn"
                )
                val (kept, putBack) = chooseAnyNumberSplit(
                    from = chosen,
                    prompt = "Pay 4 life for each card you keep; the rest go on top of your library",
                    selectedLabel = "Keep (pay 4 life each)",
                    remainderLabel = "Put on top of your library",
                    restrictions = listOf(SelectionRestriction.MaxAffordablePayment(lifePerSelected = 4)),
                )
                run(Effects.ForEachInCollection(kept, Effects.PayLife(4)))
                toLibraryTop(putBack)
            },
            prompt = "Draw two additional cards with Sylvan Library?"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "Harold McNeill"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f486df00-7c4a-4ff0-bb0b-c8b5432ac742.jpg?1783948043"
    }
}
