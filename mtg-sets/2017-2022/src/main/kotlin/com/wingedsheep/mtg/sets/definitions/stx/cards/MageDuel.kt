package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Mage Duel — Strixhaven: School of Mages #137 (canonical printing)
 * {2}{G} · Sorcery
 *
 * This spell costs {2} less to cast if you've cast another instant or sorcery spell this turn.
 * Target creature you control gets +1/+2 until end of turn. Then it fights target creature you
 * don't control.
 *
 * The discount is the Focus the Mind shape: a self-cast [ModifySpellCost] gated on
 * `YouCastSpellsThisTurn(atLeast = 1, InstantOrSorcery)`, read at cast time before this spell is
 * counted — so "another". The body is Epic Confrontation's pump-then-fight; if either target is
 * illegal on resolution no fight happens, and the pump still applies when only the other
 * creature is gone (per the rulings).
 */
val MageDuel = card("Mage Duel") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "This spell costs {2} less to cast if you've cast another instant or sorcery spell " +
        "this turn.\n" +
        "Target creature you control gets +1/+2 until end of turn. Then it fights target creature " +
        "you don't control. (Each deals damage equal to its power to the other.)"

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(
                Conditions.YouCastSpellsThisTurn(atLeast = 1, filter = GameObjectFilter.InstantOrSorcery)
            ),
        )
    }

    spell {
        val mine = target(TargetFilter.CreatureYouControl)
        val theirs = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        effect = Effects.ModifyStats(1, 2, mine) then Effects.Fight(mine, theirs)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "137"
        artist = "Randy Vargas"
        flavorText = "\"Remember the rules. No spells to the face!\""
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92325638-0321-4409-8910-2287c64c182e.jpg?1783927342"
        ruling("2021-04-16", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, add any cost increases, then apply any cost reductions (such as that of Mage Duel). Mage Duel's mana value is always 3, no matter what the total cost to cast it was.")
        ruling("2021-04-16", "You can't cast Mage Duel unless you choose both a creature you control and a creature you don't control as targets.")
        ruling("2021-04-16", "If the creature you control is an illegal target as Mage Duel tries to resolve, that creature won't get +1/+2. If that creature is a legal target, but the one you don't control isn't, your creature will still get +1/+2. In both cases, no fight will happen and no creature will deal or be dealt damage.")
    }
}
