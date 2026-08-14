package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Raging Battle Mouse
 * {1}{R}
 * Creature — Mouse
 * 2/1
 *
 * The second spell you cast each turn costs {1} less to cast.
 * Celebration — At the beginning of combat on your turn, if two or more nonland permanents entered
 * the battlefield under your control this turn, target creature you control gets +1/+1 until end
 * of turn.
 *
 * Two independent abilities:
 *
 * 1. The cost reduction is the [UthrosPsionicist][com.wingedsheep.mtg.sets.definitions.eoe.cards.UthrosPsionicist]
 *    shape — [CostGating.NthOfTypePerTurn] with `n = 2` over an untyped
 *    [SpellCostTarget.YouCast], which counts the spell currently being cast. Being a static ability
 *    of a permanent it only functions on the battlefield, so the Mouse can never discount itself
 *    even when it *is* your second spell (2023-09-01 ruling), and [CostModification.ReduceGeneric]
 *    touches only the generic component of the cost.
 *
 * 2. The Celebration trigger is exactly [BelligerentOfTheBall]'s: an intervening-'if' clause
 *    (CR 603.4) on [Conditions.Celebration], checked when the begin-combat step starts and again on
 *    resolution. The pump can target the Mouse itself.
 */
val RagingBattleMouse = card("Raging Battle Mouse") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Mouse"
    power = 2
    toughness = 1
    oracleText = "The second spell you cast each turn costs {1} less to cast.\n" +
        "Celebration — At the beginning of combat on your turn, if two or more nonland permanents " +
        "entered the battlefield under your control this turn, target creature you control gets " +
        "+1/+1 until end of turn."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any),
            modification = CostModification.ReduceGeneric(1),
            gating = CostGating.NthOfTypePerTurn(2),
        )
    }

    triggeredAbility {
        trigger = Triggers.BeginCombat
        triggerCondition = Conditions.Celebration
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.ModifyStats(power = 1, toughness = 1, target = creature)
        description = "At the beginning of combat on your turn, if two or more nonland permanents " +
            "entered the battlefield under your control this turn, target creature you control " +
            "gets +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "143"
        artist = "Rudy Siswanto"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d0ab162-540e-4999-902c-9dacd6687aca.jpg?1783915090"

        ruling(
            "2023-09-01",
            "Spells that were cast before Raging Battle Mouse entered the battlefield count. If " +
                "Raging Battle Mouse was the first spell you cast this turn, the next spell you " +
                "cast this turn is your second spell."
        )
        ruling(
            "2023-09-01",
            "Raging Battle Mouse can't reduce its own cost, even if it's the second spell you cast " +
                "in a turn."
        )
        ruling(
            "2023-09-01",
            "Raging Battle Mouse's cost-reduction ability can't reduce the amount of colored mana " +
                "you pay for a spell. It reduces only the generic mana component of that cost."
        )
    }
}
