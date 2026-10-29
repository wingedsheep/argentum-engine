package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Rakdos, Lord of Riots {B}{B}{R}{R} — Legendary Creature — Demon 6/6
 *
 * You can't cast Rakdos unless an opponent lost life this turn.
 * Flying, trample
 * Creature spells you cast cost {1} less to cast for each 1 life your opponents have lost this turn.
 *
 * The cast restriction gates casting only — Rakdos can still be put onto the battlefield by other
 * effects. The discount is a battlefield-sourced [SpellCostTarget.YouCast] reduction summing every
 * opponent's life lost this turn (Bloodsoaked Insight's amount); it reduces generic mana only, and
 * doesn't apply to casting Rakdos itself since the static only functions on the battlefield.
 */
val RakdosLordOfRiots = card("Rakdos, Lord of Riots") {
    manaCost = "{B}{B}{R}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Demon"
    power = 6
    toughness = 6
    oracleText = "You can't cast Rakdos unless an opponent lost life this turn.\n" +
        "Flying, trample\n" +
        "Creature spells you cast cost {1} less to cast for each 1 life your opponents have lost this turn."

    spell {
        castOnlyIf(Conditions.OpponentLostLifeThisTurn)
    }

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Creature),
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.lifeLostThisTurn(Player.EachOpponent)),
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "187"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04f3db71-802f-488c-b40d-ac90df2d660a.jpg?1783940334"
        ruling("2020-11-10", "Rakdos can be put onto the battlefield by another spell or ability even if no opponent has lost life that turn.")
        ruling("2020-11-10", "Rakdos's last ability cares about the total life lost, not necessarily what an opponent's life total is compared to what it was at the beginning of the turn. For example, if an opponent loses 5 life and then gains 10 life in a turn, creature spells you cast will cost {5} less to cast.")
        ruling("2020-11-10", "Rakdos's last ability can't reduce the colored mana requirement of a creature spell you cast.")
        ruling("2020-11-10", "Rakdos's last ability won't reduce the cost to cast Rakdos itself. It applies only to creature spells you cast once Rakdos is on the battlefield.")
    }
}
