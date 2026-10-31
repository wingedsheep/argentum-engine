package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Into the Story
 * {5}{U}{U}
 * Instant
 *
 * This spell costs {3} less to cast if an opponent has seven or more cards in their graveyard.
 * Draw four cards.
 *
 * "An opponent has seven or more" is existential per opponent, so the gate reads the *greatest*
 * graveyard size among the caster's opponents. `min(1, max(0, greatest - 6))` is exactly 1 when
 * some opponent has seven or more cards and 0 otherwise, so the reduction is that step times 3 —
 * never more than {3}, however many opponents qualify (per the 2019-10-04 ruling).
 */
val IntoTheStory = card("Into the Story") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "This spell costs {3} less to cast if an opponent has seven or more cards in their graveyard.\n" +
        "Draw four cards."

    staticAbility {
        val greatestOpponentGraveyard = DynamicAmounts.greatestAmongPlayers(
            DynamicAmounts.cardsInYourGraveyard(),
            Player.EachOpponent,
        )
        val sevenOrMore = DynamicAmounts.min(
            DynamicAmounts.fixed(1),
            DynamicAmounts.nonNegative(greatestOpponentGraveyard - 6),
        )
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(sevenOrMore * 3),
            ),
        )
    }

    spell {
        effect = Effects.DrawCards(4)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Jason Rainville"
        flavorText = "The boundary between real and imaginary is as thin as the page, and just as easily torn."
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c838f08e-7fb6-46e7-83bb-dce8877d6c87.jpg?1783932655"
        ruling(
            "2019-10-04",
            "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, add any cost increases, then apply any cost reductions (such as that of Into the Story). The mana value of the spell remains unchanged, no matter what the total cost to cast it was."
        )
        ruling(
            "2019-10-04",
            "Into the Story's cost reduction can only reduce its cost by {3}. It doesn't reduce the cost further if more than one opponent has seven cards in their graveyard or if an opponent has fourteen cards in their graveyard."
        )
    }
}
