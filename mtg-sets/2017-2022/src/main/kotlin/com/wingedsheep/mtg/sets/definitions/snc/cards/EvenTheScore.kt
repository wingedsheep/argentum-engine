package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Even the Score
 * {X}{U}{U}{U}
 * Instant
 * This spell costs {U}{U}{U} less to cast if an opponent has drawn four or more cards this turn.
 * Draw X cards.
 *
 * The reduction is a self-cast [ModifySpellCost] removing the three colored pips, gated on "an
 * opponent has drawn four or more cards this turn". That gate is an *existential* over opponents,
 * so it reads [DynamicAmounts.greatestAmongPlayers] over `Player.EachOpponent` (each opponent
 * measured on its own, `Player.You` rebinding to the measured player) rather than summing the
 * `CARDS_DRAWN` tracker across opponents — two opponents on two draws each must not qualify.
 */
val EvenTheScore = card("Even the Score") {
    manaCost = "{X}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "This spell costs {U}{U}{U} less to cast if an opponent has drawn four or more cards this turn.\n" +
        "Draw X cards."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceColored("{U}{U}{U}"),
            gating = CostGating.OnlyIf(
                Conditions.CompareAmounts(
                    DynamicAmounts.greatestAmongPlayers(
                        DynamicAmounts.cardsDrawnThisTurn(Player.You),
                        players = Player.EachOpponent
                    ),
                    ComparisonOperator.GTE,
                    4
                )
            )
        )
    }

    spell {
        effect = Effects.DrawCards(DynamicAmounts.castX())
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "42"
        artist = "Greg Opalinski"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c629217f-4a10-4d92-86f8-22dc4067a724.jpg?1783923145"
    }
}
