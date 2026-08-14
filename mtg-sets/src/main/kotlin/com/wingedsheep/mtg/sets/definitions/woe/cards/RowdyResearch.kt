package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Rowdy Research
 * {6}{U}
 * Instant
 *
 * This spell costs {1} less to cast for each creature that attacked this turn.
 * Draw three cards.
 *
 * The reduction is [CostReductionSource.CreaturesThatAttackedThisTurn] — the same turn-history
 * count Witchstalker Frenzy uses, unioned over every player's attackers for every combat phase
 * this turn rather than scanned off the live battlefield. That matters here for the same reason:
 * this is an instant you cast late, often after blockers or damage, when the attackers that paid
 * for it may already be in a graveyard.
 *
 * [CostModification.ReduceGenericBy] only eats generic mana, so however many creatures attacked,
 * the cost bottoms out at {U}, and the reduction never touches the mana value — a Rowdy Research
 * on the stack is always mana value 7.
 */
val RowdyResearch = card("Rowdy Research") {
    manaCost = "{6}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "This spell costs {1} less to cast for each creature that attacked this turn.\n" +
        "Draw three cards."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.CreaturesThatAttackedThisTurn()
            ),
        )
    }

    spell {
        effect = Effects.DrawCards(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "312"
        artist = "Bram Sels"
        flavorText = "\"Stolen treasures can be reclaimed, but stolen knowledge is yours forever.\"\n—Alela"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89b7e901-5ba4-4374-9eeb-96354279a123.jpg?1783915040"
    }
}
